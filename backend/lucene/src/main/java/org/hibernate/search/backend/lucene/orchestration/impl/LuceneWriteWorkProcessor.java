/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.reporting.IndexFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.IndexFailureContextImpl;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * A thread-unsafe component responsible for applying write works to an index writer.
 * <p>
 * Ported from Search 5's LuceneBackendQueueTask, in particular.
 */
public class LuceneWriteWorkProcessor implements BatchingExecutor.WorkProcessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext indexEventContext;
	private final IndexWriterDelegator indexWriterDelegator;
	private final LuceneWriteWorkExecutionContextImpl context;
	private final FailureHandler failureHandler;

	private List<LuceneWriteWork<?>> uncommittedWorks = new ArrayList<>();

	private boolean workSetForcesCommit;
	private IndexFailureContextImpl.Builder workSetFailureContextBuilder;

	public LuceneWriteWorkProcessor(EventContext indexEventContext, IndexWriterDelegator indexWriterDelegator,
			FailureHandler failureHandler) {
		this.indexEventContext = indexEventContext;
		this.indexWriterDelegator = indexWriterDelegator;
		this.context = new LuceneWriteWorkExecutionContextImpl( indexEventContext, indexWriterDelegator );
		this.failureHandler = failureHandler;
	}

	@Override
	public void beginBatch() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> endBatch() {
		try {
			commitIfNecessary();
		}
		catch (RuntimeException e) {
			IndexFailureContextImpl.Builder failureContextBuilder = new IndexFailureContextImpl.Builder();
			failureContextBuilder.throwable( e );
			failureContextBuilder.failingOperation( "Commit after a batch of index works" );
			for ( LuceneWriteWork<?> work : uncommittedWorks ) {
				failureContextBuilder.uncommittedOperation( work.getInfo() );
			}
			try {
				cleanUpAfterError();
			}
			catch (RuntimeException e2) {
				e.addSuppressed( e2 );
			}
			failureHandler.handle( failureContextBuilder.build() );
		}
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	public void beforeWorkSet(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		workSetForcesCommit = DocumentCommitStrategy.FORCE.equals( commitStrategy )
				// We need to commit in order to make the changes visible
				// TODO HSEARCH-3117 this may not be true with the NRT implementation from Search 5
				|| DocumentRefreshStrategy.FORCE.equals( refreshStrategy );
		workSetFailureContextBuilder = null;
	}

	/**
	 * This bypasses the normal {@link #submit(LuceneWriteWork)} method in order
	 * to avoid setting {@link #hasUncommittedWorks} to {@code true},
	 * so that we skip the end-of-batch commit and thus avoid the creation of an IndexWriter,
	 * which would be pointless in this case.
	 */
	void ensureIndexExists() {
		try {
			indexWriterDelegator.ensureIndexExists();
		}
		catch (IOException | RuntimeException e) {
			workSetFailureContextBuilder = new IndexFailureContextImpl.Builder();
			workSetFailureContextBuilder.throwable( log.unableToInitializeIndexDirectory(
					e.getMessage(), indexEventContext, e
			) );
			workSetFailureContextBuilder.failingOperation( "Index initialization" );
		}
	}

	public <T> T submit(LuceneWriteWork<T> work) {
		uncommittedWorks.add( work );
		if ( workSetFailureContextBuilder != null ) {
			// Skip the work: a previous work in the workset failed, so we'll give up on all works in this workset.
			return null;
		}
		try {
			return work.execute( context );
		}
		catch (RuntimeException e) {
			workSetFailureContextBuilder = new IndexFailureContextImpl.Builder();
			workSetFailureContextBuilder.throwable( e );
			workSetFailureContextBuilder.failingOperation( work.getInfo() );
			return null;
		}
	}

	public <T> void afterWorkSet(CompletableFuture<T> future, T resultIfSuccess) {
		if ( workSetFailureContextBuilder == null && workSetForcesCommit ) {
			try {
				commitIfNecessary();
			}
			catch (RuntimeException e) {
				workSetFailureContextBuilder = new IndexFailureContextImpl.Builder();
				workSetFailureContextBuilder.throwable( e );
				workSetFailureContextBuilder.failingOperation( "Commit after a set of index works" );
			}
		}
		if ( workSetFailureContextBuilder != null ) {
			// TODO HSEARCH-1375 report the index name?
			//failureContextBuilder.indexManager( resources.getIndexManager() );
			for ( LuceneWriteWork<?> work : uncommittedWorks ) {
				workSetFailureContextBuilder.uncommittedOperation( work.getInfo() );
			}
			IndexFailureContext failureContext = workSetFailureContextBuilder.build();
			workSetFailureContextBuilder = null;
			try {
				cleanUpAfterError();
			}
			catch (RuntimeException e) {
				failureContext.getThrowable().addSuppressed( e );
			}
			future.completeExceptionally( failureContext.getThrowable() );
			failureHandler.handle( failureContext );
		}
		else {
			future.complete( resultIfSuccess );
		}
	}

	private void commitIfNecessary() {
		if ( !uncommittedWorks.isEmpty() ) {
			try {
				// TODO HSEARCH-3117 restore the commit policy feature to allow scheduled commits?
				indexWriterDelegator.commit();
				// Only clear the list after the commit succeeds:
				// if it fails, we want to report uncommitted works.
				uncommittedWorks.clear();
			}
			catch (RuntimeException | IOException e) {
				throw log.unableToCommitIndex( indexEventContext, e );
			}
		}
	}

	private void cleanUpAfterError() {
		try {
			uncommittedWorks.clear();
			/*
			 * Note this will close the index writer,
			 * which with the default settings will trigger a commit.
			 */
			indexWriterDelegator.forceLockRelease();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCleanUpAfterError( indexEventContext, e );
		}
	}
}
