/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This <code>ReaderProvider</code> shares IndexReaders as long as they are "current";
 * It uses IndexReader.reopen() which should improve performance on larger indexes
 * as it shares buffers with previous IndexReader generation for the segments which didn't change.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class SharingBufferReaderProvider implements DirectoryBasedReaderProvider {

	private static final Log log = LoggerFactory.make();

	/**
	 * contains all Readers (most current per Directory and all unclosed old readers)
	 */
	protected final Map<IndexReader, ReaderUsagePair> allReaders = new ConcurrentHashMap<IndexReader, ReaderUsagePair>();

	/**
	 * contains last updated Reader; protected by lockOnOpenLatest (in the values)
	 */
	protected final Map<Directory, PerDirectoryLatestReader> currentReaders = new ConcurrentHashMap<Directory, PerDirectoryLatestReader>();

	/**
	 * Each actual IndexReader refresh will change this value. To what value exactly doesn't matter,
	 * as long as it's not a value recently used, so we don't care for overflow conditions.
	 * When a client needs to check for index freshness a lock is acquired to protect from
	 * too many checks (which result in IO operations); when it is actually able to acquire
	 * this lock it should check if the refreshOperationId changed: if so, a refresh can
	 * be skipped and we release the lock quickly as the IndexReader was then for
	 * sure updated in the time interval between this client arriving to request a reader
	 * and actually being able to get one.
	 */
	private volatile int refreshOperationId = 0;

	private DirectoryProvider directoryProvider;
	private String indexName;

	@Override
	public IndexReader openIndexReader() {
		log.debugf( "Opening IndexReader for directoryProvider %s", indexName );
		Directory directory = directoryProvider.getDirectory();
		PerDirectoryLatestReader directoryLatestReader = currentReaders.get( directory );
		// might eg happen for FSSlaveDirectoryProvider or for mutable SearchFactory
		if ( directoryLatestReader == null ) {
			directoryLatestReader = createReader( directory );
		}
		return directoryLatestReader.refreshAndGet();
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		if ( reader == null ) {
			return;
		}
		log.debugf( "Closing IndexReader: %s", reader );
		ReaderUsagePair container = allReaders.get( reader );
		container.close(); //virtual
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
		this.directoryProvider = indexManager.getDirectoryProvider();
		this.indexName = indexManager.getIndexName();
		// Initialize at least one, don't forget directoryProvider might return different Directory later
		createReader( directoryProvider.getDirectory() );
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

	@Override
	public void stop() {
		for ( IndexReader reader : allReaders.keySet() ) {
			ReaderUsagePair usage = allReaders.get( reader );
			usage.close();
		}

		if ( allReaders.size() != 0 ) {
			log.readersNotProperlyClosedInReaderProvider();
		}
	}

	//overridable method for testability:
	protected IndexReader readerFactory(final Directory directory) throws IOException {
		return IndexReader.open( directory );
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
			initialPair.usageCounter.set( 1 ); //a token to mark as active (preventing real close).
			lockOnReplaceCurrent.lock(); //no harm, just ensuring safe publishing.
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
			final IndexReader updatedReader;
			//it's important that we read this volatile before acquiring the lock:
			final int preAcquireVersionId = refreshOperationId;
			ReaderUsagePair toCloseReaderPair = null;
			lockOnReplaceCurrent.lock();
			final IndexReader beforeUpdateReader = current.reader;
			try {
				if ( refreshOperationId != preAcquireVersionId ) {
					// We can take a good shortcut
					current.usageCounter.incrementAndGet();
					return beforeUpdateReader;
				}
				else {
					try {
						//Guarded by the lockOnReplaceCurrent of current IndexReader
						//technically the final value doesn't even matter, as long as we change it
						refreshOperationId++;
						updatedReader = IndexReader.openIfChanged( beforeUpdateReader );
					}
					catch (IOException e) {
						throw new SearchException( "Unable to reopen IndexReader", e );
					}
				}
				if ( updatedReader == null ) {
					current.usageCounter.incrementAndGet();
					return beforeUpdateReader;
				}
				else {
					ReaderUsagePair newPair = new ReaderUsagePair( updatedReader );
					//no need to increment usageCounter in newPair, as it is constructed with correct number 2.
					assert newPair.usageCounter.get() == 2;
					toCloseReaderPair = current;
					current = newPair;
					allReaders.put( updatedReader, newPair );//unfortunately still needs lock
				}
			}
			finally {
				lockOnReplaceCurrent.unlock();
			}
			// doesn't need lock:
			if ( toCloseReaderPair != null ) {
				toCloseReaderPair.close();// release a token as it's not the current any more.
			}
			return updatedReader;
		}
	}

}
