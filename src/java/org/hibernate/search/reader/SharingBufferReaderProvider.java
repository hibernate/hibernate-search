// $Id$
package org.hibernate.search.reader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.LoggerFactory;

/**
 * As does SharedReaderProvider this also shares IndexReaders as long as they are "current";
 * main difference with SharedReaderProvider is the way to update the Readers when needed:
 * this uses IndexReader.reopen() which should improve performance on larger indexes
 * as it shares buffers with previous IndexReader generation for the segments which didn't change.
 * Current drawbacks are: need of Lucene > 2.3.0 and less mature (experimental).
 * 
 * @author Sanne Grinovero
 */
public class SharingBufferReaderProvider implements ReaderProvider {

	private static final Logger log = LoggerFactory.make();	

	/**
	 * contains all Readers (most current per DP and all unclosed old) 
	 */
	//TODO ConcurrentHashMap's constructor could benefit from some hints as arguments.
	protected final Map<IndexReader,ReaderUsagePair> allReaders = new ConcurrentHashMap<IndexReader,ReaderUsagePair>();
	
	/**
	 * contains last updated Reader; protected by lockOnOpenLatest (in the values)
	 */
	protected Map<DirectoryProvider,PerDirectoryLatestReader> currentReaders;

	public void closeReader(IndexReader multiReader) {
		if ( multiReader == null ) return;
		IndexReader[] readers;
		if ( multiReader instanceof MultiReader ) {
			readers = ReaderProviderHelper.getSubReadersFromMultiReader( (MultiReader) multiReader );
		}
		else {
			throw new AssertionFailure( "Everything should be wrapped in a MultiReader" );
		}
		log.debug( "Closing MultiReader: {}", multiReader );
		for ( IndexReader reader : readers ) {
			ReaderUsagePair container = allReaders.get( reader );
			container.close();//virtual
		}
		log.trace( "IndexReader closed." );
	}

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
		Map<DirectoryProvider,PerDirectoryLatestReader> map = new HashMap<DirectoryProvider,PerDirectoryLatestReader>();
		Set<DirectoryProvider> providers = searchFactoryImplementor.getDirectoryProviders();
		for ( DirectoryProvider provider : providers ) {
			try {
				map.put( provider, new PerDirectoryLatestReader( provider ) );
			} catch (IOException e) {
				throw new SearchException( "Unable to open Lucene IndexReader", e );
			}
		}
		//FIXME I'm not convinced this non-final fields are safe without locks, but I may be wrong.
		currentReaders = Collections.unmodifiableMap( map );
	}

	public void destroy() {
		IndexReader[] readers = allReaders.keySet().toArray( new IndexReader[allReaders.size()] );
		for (IndexReader reader : readers) {
			ReaderUsagePair usage =  allReaders.get( reader );
			usage.close();
		}

		if ( allReaders.size() != 0 ) log.warn( "ReaderProvider contains readers not properly closed at destroy time" );
	}

	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		int length = directoryProviders.length;
		IndexReader[] readers = new IndexReader[length];
		log.debug( "Opening IndexReader for directoryProviders: {}", length );
		for ( int index = 0; index < length; index++ ) {
			DirectoryProvider directoryProvider = directoryProviders[index];
			if ( log.isTraceEnabled() ) {
				log.trace( "Opening IndexReader from {}", directoryProvider.getDirectory() );
			}
			PerDirectoryLatestReader directoryLatestReader = currentReaders.get( directoryProvider );
			readers[index] = directoryLatestReader.refreshAndGet();
		}
		// don't use ReaderProviderHelper.buildMultiReader as we need our own cleanup.
		if ( length == 0 ) {
			return null;
		}
		else {
			try {
				return new CacheableMultiReader( readers );
			}
			catch (Exception e) {
				//Lucene 2.2 used to throw IOExceptions here
				for ( IndexReader ir : readers ) {
					ReaderUsagePair readerUsagePair = allReaders.get( ir );
					readerUsagePair.close();
				}
				throw new SearchException( "Unable to open a MultiReader", e );
			}
		}
	}
	
	//overridable method for testability:
	protected IndexReader readerFactory(DirectoryProvider provider) throws IOException {
		return IndexReader.open( provider.getDirectory() );
	}

	/**
	 * Container for the couple IndexReader,UsageCounter.
	 */
	protected final class ReaderUsagePair {
		
		public final IndexReader reader;
		/**
		 * When reaching 0 (always test on change) the reader should be really
		 * closed and then discarded.
		 * Starts at 2 because:
		 * first usage token is artificial: means "current" is not to be closed (+1)
		 * additionally when creating it will be used (+1)
		 */
		protected final AtomicInteger usageCounter = new AtomicInteger( 2 );
		
		ReaderUsagePair(IndexReader r) {
			reader = r;
		}
		
		/**
		 * closes the IndexReader if no other resource is using it;
		 * in this case the reference to this container will also be removed.
		 */
		public void close() {
			int refCount = usageCounter.decrementAndGet();
			if ( refCount==0  ) {
				//TODO I've been experimenting with the idea of an async-close: didn't appear to have an interesting benefit,
				//so discarded the code. should try with bigger indexes to see if the effect gets more impressive.
				ReaderUsagePair removed = allReaders.remove( reader );//remove ourself
				try {
					reader.close();
				} catch (IOException e) {
					log.warn( "Unable to close Lucene IndexReader", e );
				}
				assert removed != null;
			}
			else if ( refCount<0 ) {
				//doesn't happen with current code, could help spotting future bugs?
				throw new AssertionFailure( "Closing an IndexReader for which you didn't own a lock-token, or somebody else which didn't own closed already." );
			}
		}
		
		public String toString(){
			return "Reader:" + this.hashCode() + " ref.count=" + usageCounter.get();
		}
		
	}
	
	/**
	 * An instance for each DirectoryProvider,
	 * establishing the association between "current" ReaderUsagePair
	 * for a DirectoryProvider and it's lock.
	 */
	protected final class PerDirectoryLatestReader {
		
		/**
		 * Reference to the most current IndexReader for a DirectoryProvider;
		 * guarded by lockOnReplaceCurrent;
		 */
		public ReaderUsagePair current; //guarded by lockOnReplaceCurrent 
		private final Lock lockOnReplaceCurrent = new ReentrantLock();
		
		/**
		 * @param provider The DirectoryProvider for which we manage the IndexReader.
		 * @throws IOException when the index initialization fails.
		 */
		public PerDirectoryLatestReader(DirectoryProvider provider) throws IOException {
			IndexReader reader = readerFactory( provider );
			ReaderUsagePair initialPair = new ReaderUsagePair( reader );
			initialPair.usageCounter.set( 1 );//a token to mark as active (preventing real close).
			lockOnReplaceCurrent.lock();//no harm, just ensuring safe publishing.
			current = initialPair;
			lockOnReplaceCurrent.unlock();
			allReaders.put( reader, initialPair );
		}

		/**
		 * Gets an updated IndexReader for the current DirectoryProvider;
		 * the index status will be checked.
		 * @return the current IndexReader if it's in sync with underlying index, a new one otherwise.
		 */
		public IndexReader refreshAndGet() {
			ReaderUsagePair previousCurrent;
			IndexReader updatedReader;
			lockOnReplaceCurrent.lock();
			try {
				IndexReader beforeUpdateReader = current.reader;
				try {
					updatedReader = beforeUpdateReader.reopen();
				} catch (IOException e) {
					throw new SearchException( "Unable to reopen IndexReader", e );
				}
				if ( beforeUpdateReader == updatedReader ) {
					previousCurrent = null;
					current.usageCounter.incrementAndGet();
				}
				else {
					ReaderUsagePair newPair = new ReaderUsagePair( updatedReader );
					//no need to increment usageCounter in newPair, as it is constructed with correct number 2.
					assert newPair.usageCounter.get() == 2;
					previousCurrent = current;
					current = newPair;
					allReaders.put( updatedReader, newPair );//unfortunately still needs lock
				}
			} finally {
				lockOnReplaceCurrent.unlock();
			}
			// doesn't need lock:
			if ( previousCurrent != null ) {
				previousCurrent.close();// release a token as it's not the current any more.
			}
			return updatedReader;
		}
		
	}
	
}
