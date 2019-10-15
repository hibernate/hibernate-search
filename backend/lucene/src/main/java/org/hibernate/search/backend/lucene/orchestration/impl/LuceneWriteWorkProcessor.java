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
import org.hibernate.search.util.common.AssertionFailure;
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

	private List<LuceneWriteWork<?>> previousWorkSetsUncommittedWorks = new ArrayList<>();

	private boolean workSetForcesCommit;
	private List<LuceneWriteWork<?>> workSetUncommittedWorks = new ArrayList<>();
	private boolean workSetHasFailure;

	public LuceneWriteWorkProcessor(EventContext indexEventContext, IndexWriterDelegator indexWriterDelegator,
			FailureHandler failureHandler) {
		this.indexEventContext = indexEventContext;
		this.indexWriterDelegator = indexWriterDelegator;
		this.context = new LuceneWriteWorkExecutionContextImpl( indexEventContext, indexWriterDelegator );
		this.failureHandler = failureHandler;
	}

	// FIXME HSEARCH-3735 This is temporary and should be removed when failures are reported to the mapper directly
	public FailureHandler getFailureHandler() {
		return failureHandler;
	}

	@Override
	public void beginBatch() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> endBatch() {
		if ( !previousWorkSetsUncommittedWorks.isEmpty() ) {
			try {
				commit();
			}
			catch (RuntimeException e) {
				cleanUpAfterFailure( e, "Commit after a batch of index works" );
				// The exception was reported to the failure handler, no need to propagate it.
			}
			finally {
				// Only clear the lists after the commit succeeds or failures are reported.
				previousWorkSetsUncommittedWorks.clear();
			}
		}
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	public void beforeWorkSet(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		workSetForcesCommit = DocumentCommitStrategy.FORCE.equals( commitStrategy )
				// We need to commit in order to make the changes visible
				// TODO HSEARCH-3117 this may not be true with the NRT implementation from Search 5
				|| DocumentRefreshStrategy.FORCE.equals( refreshStrategy );
		workSetUncommittedWorks.clear();
		workSetHasFailure = false;
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
			throw log.unableToInitializeIndexDirectory(
					e.getMessage(), indexEventContext, e
			);
		}
	}

	public <T> T submit(LuceneWriteWork<T> work) {
		if ( workSetHasFailure ) {
			throw new AssertionFailure(
					"A work was submitted to the processor after a failure occurred in the current workset."
							+ " There is a bug in Hibernate Search, please report it."
			);
		}
		try {
			workSetUncommittedWorks.add( work );
			return work.execute( context );
		}
		catch (RuntimeException e) {
			cleanUpAfterFailure( e, work.getInfo() );
			throw e;
		}
	}

	public void afterSuccessfulWorkSet() {
		if ( workSetForcesCommit ) {
			try {
				commit();
			}
			catch (RuntimeException e) {
				cleanUpAfterFailure( e, "Commit after a set of index works" );
				throw e;
			}
			finally {
				// Only clear the lists after the commit succeeds or failures are reported.
				previousWorkSetsUncommittedWorks.clear();
				workSetUncommittedWorks.clear();
			}
		}
		else {
			previousWorkSetsUncommittedWorks.addAll( workSetUncommittedWorks );
			workSetUncommittedWorks.clear();
		}
	}

	private void commit() {
		try {
			// TODO HSEARCH-3117 restore the commit policy feature to allow scheduled commits?
			indexWriterDelegator.commit();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCommitIndex( indexEventContext, e );
		}
	}

	private void cleanUpAfterFailure(Throwable throwable, Object failingOperation) {
		try {
			/*
			 * Note this will close the index writer,
			 * which with the default settings will trigger a commit.
			 */
			indexWriterDelegator.forceLockRelease();
		}
		catch (RuntimeException | IOException e) {
			throwable.addSuppressed( log.unableToCleanUpAfterError( indexEventContext, e ) );
		}

		if ( previousWorkSetsUncommittedWorks.isEmpty() ) {
			// The failure will be reported elsewhere with all the necessary context.
			return;
		}

		/*
		 * The failure will be reported elsewhere,
		 * but that report will not mention that some works from previous worksets may have been affected too.
		 * Report the failure again, just to warn about previous worksets potentially being affected.
		 */
		IndexFailureContext.Builder failureContextBuilder = IndexFailureContext.builder();
		failureContextBuilder.throwable( throwable );
		failureContextBuilder.failingOperation( failingOperation );
		for ( LuceneWriteWork<?> work : previousWorkSetsUncommittedWorks ) {
			failureContextBuilder.uncommittedOperation( work.getInfo() );
		}
		previousWorkSetsUncommittedWorks.clear();
		IndexFailureContext failureContext = failureContextBuilder.build();
		failureHandler.handle( failureContext );
	}
}
