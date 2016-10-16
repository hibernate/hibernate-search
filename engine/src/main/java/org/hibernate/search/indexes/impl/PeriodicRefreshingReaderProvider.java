/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This <code>ReaderProvider</code> shares IndexReader buffers among threads and
 * will return "recently" refreshed <code>ReaderProvider</code>, which implies
 * query results might represent the state of a slightly out of date index.
 *
 * The definition of "recently" refreshed is defined by configuration properties,
 * so the user can choose how much the index being out of date is acceptable.
 *
 * In exchange of allowing slightly out of date query results, the fact that we
 * can cap the frequency of index reopening events can provide significant
 * performance improvements in some architectures.
 *
 * @author Sanne Grinovero (C) 2016 Red Hat Inc.
 */
public class PeriodicRefreshingReaderProvider implements DirectoryBasedReaderProvider {

	public static final int DEFAULT_ACCEPTABLE_MAX_AGE_MS = 5000;
	private static final Log log = LoggerFactory.make();

	/**
	 * contains all Readers (most current per Directory and all unclosed old readers)
	 */
	protected final Map<IndexReader, ReaderUsagePair> allReaders = new ConcurrentHashMap<IndexReader, ReaderUsagePair>();

	/**
	 * contains last updated Reader; protected by lockOnOpenLatest (in the values)
	 */
	protected final Map<Directory, PerDirectoryLatestReader> currentReaders = new ConcurrentHashMap<Directory, PerDirectoryLatestReader>();

	private volatile ScheduledExecutorService scheduledExecutorService;
	private int delay;

	private DirectoryProvider directoryProvider;
	private String indexName;

	@Override
	public IndexReader openIndexReader() {
		log.tracef( "Opening IndexReader for directoryProvider %s", indexName );
		Directory directory = directoryProvider.getDirectory();
		PerDirectoryLatestReader directoryLatestReader = currentReaders.get( directory );
		// might eg happen for FSSlaveDirectoryProvider or for mutable SearchFactory
		if ( directoryLatestReader == null ) {
			directoryLatestReader = createReader( directory );
		}
		return directoryLatestReader.getLatestReader();
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		if ( reader == null ) {
			return;
		}
		log.tracef( "Closing IndexReader: %s", reader );
		ReaderUsagePair container = allReaders.get( reader );
		container.close(); //virtual
	}

	//overridable method for testability:
	protected DirectoryReader readerFactory(final Directory directory) throws IOException {
		return DirectoryReader.open( directory );
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
		this.directoryProvider = indexManager.getDirectoryProvider();
		this.indexName = indexManager.getIndexName();
		// Initialize at least one, don't forget directoryProvider might return a different Directory later
		createReader( directoryProvider.getDirectory() );
		this.scheduledExecutorService = Executors.newScheduledThreadPool( "Periodic IndexReader refreshing task for index " + indexName );
		this.delay = ConfigurationParseHelper.getIntValue(
				props,
				Environment.ASYNC_READER_REFRESH_PERIOD_MS,
				DEFAULT_ACCEPTABLE_MAX_AGE_MS );
		this.scheduledExecutorService.scheduleAtFixedRate(
				new IndexRefreshTask(),
				delay,
				delay,
				TimeUnit.MILLISECONDS
		);
	}

	@Override
	public void stop() {
		if ( scheduledExecutorService != null ) {
			try {
				ScheduledExecutorService executorService = scheduledExecutorService;
				scheduledExecutorService = null;
				executorService.shutdown();
				executorService.awaitTermination( 1, TimeUnit.SECONDS );
			}
			catch (InterruptedException e) {
				log.timedOutWaitingShutdownOfReaderProvider( indexName );
			}
		}
		for ( IndexReader reader : allReaders.keySet() ) {
			ReaderUsagePair usage = allReaders.get( reader );
			usage.close();
		}

		if ( allReaders.size() != 0 ) {
			log.readersNotProperlyClosedInReaderProvider();
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
		catch (IOException e) {
			throw new SearchException( "Unable to open Lucene IndexReader for IndexManager " + this.indexName, e );
		}
	}


	/**
	 * Container for the couple IndexReader,UsageCounter.
	 */
	protected final class ReaderUsagePair {

		public final DirectoryReader reader;
		/**
		 * When reaching 0 (always test on change) the reader should be really
		 * closed and then discarded.
		 * Starts at 2 because:
		 * first usage token is artificial: means "current" is not to be closed (+1)
		 * additionally when creating it will be used (+1)
		 */
		protected final AtomicInteger usageCounter = new AtomicInteger( 2 );

		ReaderUsagePair(DirectoryReader r) {
			reader = r;
		}

		/**
		 * Closes the <code>IndexReader</code> if no other resource is using it
		 * in which case the reference to this container will also be removed.
		 */
		public void close() {
			int refCount = usageCounter.decrementAndGet();
			if ( refCount == 0 ) {
				ReaderUsagePair removed = allReaders.remove( reader );//remove ourself
				try {
					reader.close();
				}
				catch (IOException e) {
					log.unableToCloseLuceneIndexReader( e );
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

		@Override
		public String toString() {
			return "Reader:" + this.hashCode() + " ref.count=" + usageCounter.get();
		}

	}

	/**
	 * An instance for each DirectoryProvider, pointing to the "current" ReaderUsagePair
	 * for each DirectoryProvider.
	 */
	protected final class PerDirectoryLatestReader {

		/**
		 * Reference to the most current IndexReader for a DirectoryProvider
		 * guarded by read/write locks rl and wl.
		 */
		private ReaderUsagePair current;

		/**
		 * Read/Write locks to ensure the current IndexReader isn't closed while
		 * it's being grabbed for usage.
		 */
		private final Lock rl;
		private final Lock wl;

		/**
		 * @param directory The <code>Directory</code> for which we manage the <code>IndexReader</code>.
		 *
		 * @throws IOException when the index initialization fails.
		 */
		public PerDirectoryLatestReader(Directory directory) throws IOException {
			ReadWriteLock rwl = new ReentrantReadWriteLock();
			rl = rwl.readLock();
			wl = rwl.writeLock();
			DirectoryReader reader = readerFactory( directory );
			ReaderUsagePair initialPair = new ReaderUsagePair( reader );
			initialPair.usageCounter.set( 1 ); //a token to mark as active (preventing real close).
			wl.lock();
			current = initialPair;
			wl.unlock();//only to ensure safe publication of 'current' to requestors using the lock.
			allReaders.put( reader, initialPair );
		}

		public DirectoryReader getLatestReader() {
			rl.lock();
			ReaderUsagePair readerUsagePair = current;
			readerUsagePair.usageCounter.incrementAndGet();
			rl.unlock();
			return readerUsagePair.reader;
		}

		public synchronized void refreshIfNeeded() {
			final ReaderUsagePair readerUsagePair = current;
			final DirectoryReader beforeUpdateReader = readerUsagePair.reader;
			final DirectoryReader updatedReader;
			try {
				updatedReader = DirectoryReader.openIfChanged( beforeUpdateReader );
			}
			catch (IOException e) {
				throw new SearchException( "Unable to reopen IndexReader", e );
			}
			if ( updatedReader != null ) {
				//In this case we need to promote the updated as "current"
				//and start the lazy closing process of the previous one.
				ReaderUsagePair newPair = new ReaderUsagePair( updatedReader );
				allReaders.put( updatedReader, newPair );
				//Acquire the write-lock, both for visibility reasons and to
				//make sure a client can get it and increment the usage counter atomically.
				try {
					wl.lock();
					current = newPair;
					wl.unlock();
				}
				finally {
					if ( readerUsagePair != null ) {
						readerUsagePair.close();// release a token as it's not the current any more.
					}
				}
			}
		}

	}

	private final class IndexRefreshTask implements Runnable {

		@Override
		public void run() {
			for ( PerDirectoryLatestReader lr : currentReaders.values() ) {
				lr.refreshIfNeeded();
			}
		}

	}

}
