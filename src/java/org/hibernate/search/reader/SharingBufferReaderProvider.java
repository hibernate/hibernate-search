package org.hibernate.search.reader;

import static org.hibernate.search.reader.ReaderProviderHelper.buildMultiReader;
import static org.hibernate.search.reader.ReaderProviderHelper.clean;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sanne Grinovero
 */
public class SharingBufferReaderProvider implements ReaderProvider {
	
	//contains last updated Reader; protected by lockOnOpenLatest.
	private volatile ReaderUsagePair current;
	
	private final Lock lockOnOpenLatest = new ReentrantLock();
	
	//contains all older Readers:
	protected final Map<IndexReader,ReaderUsagePair> oldReaders = new ConcurrentHashMap<IndexReader,ReaderUsagePair>();
	
	private final Logger log = LoggerFactory.getLogger ( SharingBufferReaderProvider.class );

	public void closeReader(IndexReader reader) {
		if ( reader == current.reader ) {
			boolean closeit;
			lockOnOpenLatest.lock();
			try {
				if ( reader == current.reader ){
					current.usageCounter.getAndDecrement();	
					closeit = false;
				}
				else {
					closeit = true;
				}
			}
			finally {
				lockOnOpenLatest.unlock();
			}
			if ( closeit ) {
				closeOldReader( reader );
			}
		}
		else {
			closeOldReader( reader );
		}
		printState();
	}

	private void closeOldReader(IndexReader reader) {
		try {
			ReaderUsagePair pair = oldReaders.get( reader );
			boolean closed = pair.close(); //also testing "assert pair!=null";
			if ( closed ) {
				//not longer needed, so remove references:
				oldReaders.remove( reader );
				log.trace( "IndexReader closed." );
			}
			else {
				log.trace( "Closing of IndexReader skipped: still being used." );
			}
		}
		catch (IOException e) {
			log.warn( "Unable to close Lucene IndexReader", e );
			//remove references anyway:
			oldReaders.remove( reader );
		}
	}

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
		//FIXME initialize currentReaderContainer here instead of lazy
	}

	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		boolean trace = log.isTraceEnabled();
		if ( trace ) log.trace( "Opening IndexReader for directoryProviders: {}", directoryProviders );
		IndexReader toReturn;
		lockOnOpenLatest.lock();
		try {
			if ( current == null ) { //FIXME move this case to initialize
				current = initialReaderOpening( directoryProviders );
				log.trace( "IndexReader initialized." );
			}
			else {
				reopenIndexreader();
			}
			toReturn = current.reader; //choose reader before unlock
		} finally {
			lockOnOpenLatest.unlock();
		}
		printState();
		return toReturn;
	}
	
	private void reopenIndexreader() {
		// we own the lock
		IndexReader before = current.reader;
		IndexReader updatedReader;
		try {
			updatedReader = before.reopen();
		} catch (IOException e) {
			throw new SearchException( "Unable to reopen IndexReader", e );
		}
		if ( before == updatedReader ) {
			current.incrementUseCounter();
		}
		else { //store the old one for close() functionality.
			int useCount = current.usageCounter.get();
			if ( useCount != 0 ) {
				oldReaders.put( before, current );
			}
			else {
				//or close it if nobody uses.
				try {
					current.reader.close();
				} catch (IOException e) {
					log.warn( "Unable to close Lucene IndexReader", e );
				}
			}
			current = new ReaderUsagePair( updatedReader );
		}
	}
	
	public final void printState(){
		if ( log.isTraceEnabled())
			log.trace( "Current "+ current + " older:" + oldReaders.values() );
	}

	private ReaderUsagePair initialReaderOpening(DirectoryProvider[] directoryProviders) {
		// we own the lock
		final int length = directoryProviders.length;
		IndexReader[] readers = new IndexReader[length];
		try {
			for (int index = 0; index < length; index++) {
				readers[index] = IndexReader.open( directoryProviders[index].getDirectory() );
			}
		}
		catch (IOException e) {
			//TODO more contextual info
			clean( new SearchException( "Unable to open one of the Lucene indexes", e ), readers );
		}
		IndexReader iR = readerFactory( length, readers );
		return new ReaderUsagePair( iR );
	}
	
	//overridable method for testability:
	protected IndexReader readerFactory(int length, IndexReader[] readers) {
		return buildMultiReader( length, readers );
	}

	protected static class ReaderUsagePair {
		protected final IndexReader reader;
		protected final AtomicInteger usageCounter;
		
		ReaderUsagePair(IndexReader r) {
			reader = r;
			usageCounter = new AtomicInteger( 1 );
		}
		
		void incrementUseCounter() {
			usageCounter.incrementAndGet();
		}
		
		public int getUsageCount(){
			return usageCounter.get();
		}

		/**
		 * @return true when really closing the underlying IndexReader
		 * @throws IOException
		 */
		private boolean close() throws IOException {
			int count = usageCounter.decrementAndGet();
			if ( count == 0 ) {
				reader.close();
				return true;
			}
			assert count >= 0;
			return false;
		}
		
		public String toString(){
			return "Reader:"+this.hashCode()+" count="+usageCounter.get();
		}
		
	}

}
