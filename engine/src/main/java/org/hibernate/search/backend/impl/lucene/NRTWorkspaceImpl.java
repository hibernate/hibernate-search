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
package org.hibernate.search.backend.impl.lucene;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.IndexReader;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.impl.WorkVisitor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A {@code Workspace} implementation taking advantage of NRT Lucene features.
 * {@code IndexReader} instances are obtained directly from the {@code IndexWriter}, which is not forced
 * to flush all pending changes to the {@code Directory} structure.
 * <p/>
 *
 * Lucene requires in its current version to flush delete operations, or the {@code IndexReader}s
 * retrieved via NRT will include deleted Document instances in queries; flushing delete operations
 * happens to be quite expensive so this {@code Workspace} implementation attempts to detect when such
 * a flush operation is needed.
 * <p/>
 *
 * Applying write operations flags "indexReader requirements" with needs for either normal flush
 * or flush including deletes, but does not update {@code IndexReader} instances. The {@code IndexReader}s
 * are updated only if and when a fresh {@code IndexReader} is requested via {@link #openIndexReader()}.
 * This method will check if it can return the last opened {@code IndexReader} or in case of the reader being stale
 * open a fresh reader from the current {@code IndexWriter}.
 * <p/>
 *
 * Generation counters are used to track need-at-least version versus last-updated-at version:
 * shared state is avoided between index writers and reader threads to avoid high complexity.
 * The method {@link #afterTransactionApplied(boolean, boolean)} might trigger multiple times flagging
 * the index to be dirty without triggering an actual {@code IndexReader} refresh, so the version counters
 * can have gaps: method {@link #refreshReaders()} will always jump to latest seen version, as it will
 * refresh the index to satisfy both kinds of flush requirements (writes and deletes).
 * <p/>
 *
 * We keep a reference {@code IndexReader} in the {@link #currentReader} atomic reference as a fast path
 * for multiple read events when the index is not dirty.
 * <p/>
 *
 * This class implements both {@code Workspace} and {@code ReaderProvider}.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class NRTWorkspaceImpl extends AbstractWorkspaceImpl implements DirectoryBasedReaderProvider {

	private static final Log log = LoggerFactory.make();

	private final ReentrantLock writeLock = new ReentrantLock();
	private final AtomicReference<IndexReader> currentReader = new AtomicReference<IndexReader>();

	/**
	 * Visits {@code LuceneWork} types to determine the kind of flushing we need to apply on the indexes
	 */
	private final FlushStrategySelector flushStrategySelector = new FlushStrategySelector();

	/**
	 * Set to true when this service is shutdown (not revertible)
	 */
	private boolean shutdown = false;

	/**
	 * When true a flush operation should make sure all write operations are flushed,
	 * otherwise a simpler flush strategy can be picked.
	 */
	private final AtomicBoolean needFlushWrites = new AtomicBoolean( true );

	/**
	 * Often when flushing deletes don't need to be applied. Some operation might have requested otherwise:
	 */
	private final AtomicBoolean needFlushDeletes = new AtomicBoolean( false );

	/**
	 * Internal counter used to mark different generations of IndexReaders. Monotonic incremental.
	 * Not expecting an overflow in this planet's lifetime.
	 */
	private final AtomicLong readerGeneration = new AtomicLong( 0 );

	/**
	 * When refreshing an {@code IndexReader} to achieve a fresh snapshot to a generation, we need to check this
	 * value to see if deletions need to be flushed. We try hard to not flush deletions as that is a
	 * very expensive operation.
	 * NOTE: concurrently accessed. Guarded by readerGenRequiringFlushWrites: read the other first, write it last.
	 */
	private long readerGenRequiringFlushDeletes = 0;

	/**
	 * As with {@link #readerGenRequiringFlushDeletes}, if this value is above the value of {@link #currentReaderGen}
	 * a new {@code IndexReader} should be opened as the current generation is stale.
	 */
	private volatile long readerGenRequiringFlushWrites = 0;

	/**
	 * Generation identifier of the current open {@code IndexReader} (the one stored in {@link #currentReader}
	 */
	private volatile long currentReaderGen = 0;

	public NRTWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext buildContext, Properties cfg) {
		super( indexManager, buildContext, cfg );
	}

	@Override
	public void afterTransactionApplied(boolean someFailureHappened, boolean streaming) {
		if ( someFailureHappened ) {
			writerHolder.forceLockRelease();
		}
		else if ( !streaming ) {
			setupNewReadersRequirements();
		}
	}

	/**
	 * Translates fields as{@code needFlushWrites} and {@code needFlushDeletes} in a set of requirements as checked
	 * by reader threads. This is commonly invoked by a single thread (so no contention on this method
	 * is expected) but it needs to expose a consistent view of the written fields to {@link #refreshReaders()}.
	 * This is normally not invoked in parallel by multiple threads as the backend design allows a single working thread
	 * per index, but it could be invoked concurrently when streaming work is being applied (when a MassIndexer is
	 * running). Note that multiple threads invoking this in parallel might result in skipping some sequence numbers
	 * but that's not a problem.
	 */
	private void setupNewReadersRequirements() {
		if ( needFlushDeletes.get() || needFlushWrites.get() ) {
			final long nextGenId = readerGeneration.incrementAndGet();
			if ( needFlushDeletes.get() ) {
				this.needFlushDeletes.lazySet( false ); //flushed by volatile write at end of method
				this.readerGenRequiringFlushDeletes = nextGenId; //flushed by volatile write at end of method
			}
			this.needFlushWrites.lazySet( false ); //flushed by volatile write at end of method
			this.readerGenRequiringFlushWrites = nextGenId;
		}
	}

	/**
	 * Invoked when a refresh of current {@code IndexReader}s is detected necessary.
	 *
	 * The implementation is blocking to maximize reuse of a single {@code IndexReader} (better for buffer usage,
	 * caching, ..) and to avoid multiple threads trying and opening the same resources at the same time.
	 *
	 * @return the refreshed {@code IndexReader}
	 */
	private synchronized IndexReader refreshReaders() {
		//double-check for the case we don't need anymore to refresh
		if ( indexReaderIsFresh() ) {
			return currentReader.get();
		}
		//order of the following two reads DOES matter:
		final long readerGenRequiringFlushWrites = this.readerGenRequiringFlushWrites;
		final long readerGenRequiringFlushDeletes = this.readerGenRequiringFlushDeletes;
		final boolean flushDeletes = currentReaderGen < readerGenRequiringFlushDeletes;
		final long openingGen = Math.max( readerGenRequiringFlushDeletes, readerGenRequiringFlushWrites );

		final IndexReader newIndexReader = writerHolder.openNRTIndexReader( flushDeletes );
		final IndexReader oldReader = currentReader.getAndSet( newIndexReader );
		this.currentReaderGen = openingGen;
		try {
			if ( oldReader != null ) {
				oldReader.decRef();
			}
		}
		catch (IOException e) {
			log.unableToCloseLuceneIndexReader( e );
		}
		return newIndexReader;
	}

	private boolean indexReaderIsFresh() {
		final long currentReaderGen = this.currentReaderGen;
		//Note it reads the volatile first. These two longs are always updated in pairs.
		return currentReaderGen >= readerGenRequiringFlushWrites && currentReaderGen >= readerGenRequiringFlushDeletes;
	}

	@Override
	public IndexReader openIndexReader() {
		return openIndexReader( ! indexReaderIsFresh() );
	}

	/**
	 * @param needRefresh when {@code false} it won't guarantee the index reader to be affected by "latest" changes
	 * @return returns an {@code IndexReader} instance, either pooled or a new one
	 */
	private IndexReader openIndexReader(final boolean needRefresh) {
		IndexReader indexReader;
		if ( needRefresh ) {
			indexReader = refreshReaders();
		}
		else {
			indexReader = currentReader.get();
		}
		if ( indexReader == null ) {
			writeLock.lock();
			try {
				if ( shutdown ) {
					throw new AssertionFailure( "IndexReader requested after ReaderProvider is shutdown" );
				}
				indexReader = currentReader.get();
				if ( indexReader == null ) {
					indexReader = writerHolder.openDirectoryIndexReader();
					currentReader.set( indexReader );
				}
			}
			finally {
				writeLock.unlock();
			}
		}
		if ( indexReader.tryIncRef() ) {
			return indexReader;
		}
		else {
			//In this case we have a race: the chosen IndexReader was closed before we could increment its reference, so we need
			//to try again. Basically an optimistic lock as the race condition is very unlikely.
			//Changes should be tested at least with ReadWriteParallelismTest (in the performance tests module).
			//In case new writes happened there is no need to refresh again.
			return openIndexReader( false );
		}
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		if ( reader == null ) {
			return;
		}
		try {
			//don't use IndexReader#close as it prevents further counter decrements!
			reader.decRef();
		}
		catch (IOException e) {
			log.unableToCloseLuceneIndexReader( e );
		}
	}

	@Override
	public void initialize(DirectoryBasedIndexManager indexManager, Properties props) {
	}

	@Override
	public void stop() {
		writeLock.lock();
		try {
			final IndexReader oldReader = currentReader.getAndSet( null );
			closeIndexReader( oldReader );
			shutdown = true;
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void flush() {
		//Even if this is the NRT workspace, Flush is implemented as a real Flush to make sure
		//MassIndexer output is committed to permanent storage
		writerHolder.commitIndexWriter();
	}

	@Override
	public void notifyWorkApplied(LuceneWork work) {
		incrementModificationCounter();
		work.getWorkDelegate( flushStrategySelector ).apply( this );
	}

	/**
	 * Visits each kind of {@code LuceneWork} we're processing to define which kind
	 * of flushing strategy we need to apply to create consistent index readers.
	 */
	private static class FlushStrategySelector implements WorkVisitor<FlushStrategyNeed> {

		@Override
		public FlushStrategyNeed getDelegate(AddLuceneWork addLuceneWork) {
			return FlushStrategyNeed.FLUSH_WRITES;
		}

		@Override
		public FlushStrategyNeed getDelegate(DeleteLuceneWork deleteLuceneWork) {
			return FlushStrategyNeed.FLUSH_DELETIONS;
		}

		@Override
		public FlushStrategyNeed getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
			return FlushStrategyNeed.NONE;
		}

		@Override
		public FlushStrategyNeed getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
			return FlushStrategyNeed.FLUSH_DELETIONS;
		}

		@Override
		public FlushStrategyNeed getDelegate(UpdateLuceneWork updateLuceneWork) {
			return FlushStrategyNeed.FLUSH_WRITES_AND_DELETES;
		}

		@Override
		public FlushStrategyNeed getDelegate(FlushLuceneWork flushLuceneWork) {
			return FlushStrategyNeed.FLUSH_WRITES_AND_DELETES;
		}
	}

	private enum FlushStrategyNeed {
		NONE {
			@Override
			void apply(final NRTWorkspaceImpl workspace) {
			}
		},
		FLUSH_DELETIONS {
			@Override
			void apply(final NRTWorkspaceImpl workspace) {
				// AtomicBoolean#lazySet is good enough as we only want to provide reads consistent with the state
				// the application is expecting. If for example no other flush is happening down the road
				// (which will eventually flush this write too) we're fine for other cores to "see"
				// IndexReader instances slightly stale.
				workspace.needFlushDeletes.lazySet( true );
			}
		},
		FLUSH_WRITES {
			@Override
			void apply(final NRTWorkspaceImpl workspace) {
				//See FLUSH_DELETIONS for why #lazySet is good enough.
				workspace.needFlushWrites.lazySet( true );
			}
		},
		FLUSH_WRITES_AND_DELETES {
			@Override
			void apply(NRTWorkspaceImpl workspace) {
				FLUSH_DELETIONS.apply( workspace );
				FLUSH_WRITES.apply( workspace );
			}
		};
		abstract void apply(NRTWorkspaceImpl workspace);
	}

}
