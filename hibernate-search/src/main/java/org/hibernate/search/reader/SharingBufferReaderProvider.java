/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.reader;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.LoggerFactory;

/**
 * This <code>ReaderProvider</code> shares IndexReaders as long as they are "current";
 * main difference with SharedReaderProvider is the way to update the Readers when needed:
 * this uses IndexReader.reopen() which should improve performance on larger indexes
 * as it shares buffers with previous IndexReader generation for the segments which didn't change.
 *
 * @author Sanne Grinovero
 */
public class SharingBufferReaderProvider implements ReaderProvider {

	private static final Logger log = LoggerFactory.make();

	/**
	 * contains all Readers (most current per Directory and all unclosed old readers)
	 */
	//TODO ConcurrentHashMap's constructor could benefit from some hints as arguments.
	protected final Map<IndexReader, ReaderUsagePair> allReaders = new ConcurrentHashMap<IndexReader, ReaderUsagePair>();

	/**
	 * contains last updated Reader; protected by lockOnOpenLatest (in the values)
	 */
	protected final Map<Directory, PerDirectoryLatestReader> currentReaders = new ConcurrentHashMap<Directory, PerDirectoryLatestReader>();

	public void closeReader(IndexReader multiReader) {
		if ( multiReader == null ) {
			return;
		}
		IndexReader[] readers;
		if ( multiReader instanceof MultiReader ) {
			readers = ReaderProviderHelper.getSubReadersFromMultiReader( ( MultiReader ) multiReader );
		}
		else {
			throw new AssertionFailure( "Everything should be wrapped in a MultiReader" );
		}
		log.debug( "Closing MultiReader: {}", multiReader );
		for ( IndexReader reader : readers ) {
			ReaderUsagePair container = allReaders.get( reader );
			container.close(); //virtual
		}
		log.trace( "IndexReader closed." );
	}

	public void initialize(Properties props, BuildContext context) {
		Set<DirectoryProvider<?>> providers = context.getDirectoryProviders();

		// create the readers for the known providers. Unfortunately, it is not possible to
		// create all readers in initialize since some providers have more than one directory (eg
		// FSSlaveDirectoryProvider). See also HSEARCH-250.
		for ( DirectoryProvider provider : providers ) {
			createReader( provider.getDirectory() );
		}
	}

	/**
	 * Thread safe creation of <code>PerDirectoryLatestReader</code>.
	 *
	 * @param directory The Lucene directory for which to create the reader.
	 * @return either the cached instance for the specified <code>Directory</code> or a newly created one.
	 * @see <a href="http://opensource.atlassian.com/projects/hibernate/browse/HSEARCH-250">HSEARCH-250</a>
	 */
	private synchronized PerDirectoryLatestReader createReader(Directory directory) {
		PerDirectoryLatestReader reader = currentReaders.get( directory );
		if ( reader != null ) {
			return reader;
		}

		try {
			reader = new PerDirectoryLatestReader( directory );
			currentReaders.put( directory, reader );
			return reader;
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to open Lucene IndexReader", e );
		}
	}

	public void destroy() {
		IndexReader[] readers = allReaders.keySet().toArray( new IndexReader[allReaders.size()] );
		for ( IndexReader reader : readers ) {
			ReaderUsagePair usage = allReaders.get( reader );
			usage.close();
		}

		if ( allReaders.size() != 0 ) {
			log.warn( "ReaderProvider contains readers not properly closed at destroy time" );
		}
	}

	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		int length = directoryProviders.length;
		IndexReader[] readers = new IndexReader[length];
		log.debug( "Opening IndexReader for directoryProviders: {}", length );
		for ( int index = 0; index < length; index++ ) {
			Directory directory = directoryProviders[index].getDirectory();
			log.trace( "Opening IndexReader from {}", directory );
			PerDirectoryLatestReader directoryLatestReader = currentReaders.get( directory );
			// might eg happen for FSSlaveDirectoryProvider or for mutable SearchFactory
			if ( directoryLatestReader == null ) {
				directoryLatestReader = createReader( directory );
			}
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
			catch ( Exception e ) {
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
	protected IndexReader readerFactory(final Directory directory) throws IOException {
		return IndexReader.open( directory, true );
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
		 * Closes the <code>IndexReader</code> if no other resource is using it
		 * in which case the reference to this container will also be removed.
		 */
		public void close() {
			int refCount = usageCounter.decrementAndGet();
			if ( refCount == 0 ) {
				//TODO I've been experimenting with the idea of an async-close: didn't appear to have an interesting benefit,
				//so discarded the code. should try with bigger indexes to see if the effect gets more impressive.
				ReaderUsagePair removed = allReaders.remove( reader );//remove ourself
				try {
					reader.close();
				}
				catch ( IOException e ) {
					log.warn( "Unable to close Lucene IndexReader", e );
				}
				assert removed != null;
			}
			else if ( refCount < 0 ) {
				//doesn't happen with current code, could help spotting future bugs?
				throw new AssertionFailure(
						"Closing an IndexReader for which you didn't own a lock-token, or somebody else which didn't own closed already."
				);
			}
		}

		public String toString() {
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
		 * @param directory The <code>Directory</code> for which we manage the <code>IndexReader</code>.
		 *
		 * @throws IOException when the index initialization fails.
		 */
		public PerDirectoryLatestReader(Directory directory) throws IOException {
			IndexReader reader = readerFactory( directory );
			ReaderUsagePair initialPair = new ReaderUsagePair( reader );
			initialPair.usageCounter.set( 1 );//a token to mark as active (preventing real close).
			lockOnReplaceCurrent.lock();//no harm, just ensuring safe publishing.
			current = initialPair;
			lockOnReplaceCurrent.unlock();
			allReaders.put( reader, initialPair );
		}

		/**
		 * Gets an updated IndexReader for the current Directory;
		 * the index status will be checked.
		 *
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
				}
				catch ( IOException e ) {
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
			}
			finally {
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
