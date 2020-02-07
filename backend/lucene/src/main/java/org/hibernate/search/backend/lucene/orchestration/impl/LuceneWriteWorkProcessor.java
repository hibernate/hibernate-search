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
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
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

	private final String indexName;
	private final EventContext eventContext;
	private final IndexAccessor indexAccessor;
	private final LuceneWriteWorkExecutionContextImpl context;
	private final FailureHandler failureHandler;

	private List<LuceneWriteWork<?>> previousWorkSetsUncommittedWorks = new ArrayList<>();

	private boolean workSetForcesCommit;
	private boolean workSetForcesRefresh;
	private List<LuceneWriteWork<?>> workSetUncommittedWorks = new ArrayList<>();
	private boolean workSetHasFailure;

	public LuceneWriteWorkProcessor(String indexName, EventContext eventContext,
			IndexAccessor indexAccessor, FailureHandler failureHandler) {
		this.indexName = indexName;
		this.eventContext = eventContext;
		this.indexAccessor = indexAccessor;
		this.context = new LuceneWriteWorkExecutionContextImpl( eventContext, indexAccessor );
		this.failureHandler = failureHandler;
	}

	@Override
	public void beginBatch() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<Long> endBatch() {
		if ( !previousWorkSetsUncommittedWorks.isEmpty() ) {
			try {
				// TODO HSEARCH-3775 restore the commit policy feature to allow scheduled commits?
				indexAccessor.commit();
				previousWorkSetsUncommittedWorks.clear();
			}
			catch (RuntimeException e) {
				cleanUpAfterFailure( e, "Commit after a batch of index works" );
				// The exception was reported to the failure handler, no need to propagate it.
			}
		}
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public long completeOrDelay() {
		// TODO HSEARCH-3775 execute commit here and return a positive number if it's too early for a commit
		return 0;
	}

	public void beforeWorkSet(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		workSetForcesCommit = DocumentCommitStrategy.FORCE.equals( commitStrategy );
		workSetForcesRefresh = DocumentRefreshStrategy.FORCE.equals( refreshStrategy );
		workSetUncommittedWorks.clear();
		workSetHasFailure = false;
	}

	/**
	 * This bypasses the normal {@link #submit(LuceneWriteWork)} method in order
	 * to avoid appending works to {@link #workSetUncommittedWorks},
	 * so that we skip the end-of-batch commit and thus avoid the creation of an IndexWriter,
	 * which would be pointless in this case.
	 */
	void ensureIndexExists() {
		indexAccessor.ensureIndexExists();
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
				indexAccessor.commit();
				// Previous worksets were committed along with this workset
				previousWorkSetsUncommittedWorks.clear();
			}
			catch (RuntimeException e) {
				cleanUpAfterFailure( e, "Commit after a set of index works" );
				// We'll skip the refresh, but that's okay: we just reset the writer/reader anyway.
				throw e;
			}
			finally {
				// Whether the commit succeeded or not, we should not care about these works any longer
				workSetUncommittedWorks.clear();
			}
		}

		previousWorkSetsUncommittedWorks.addAll( workSetUncommittedWorks );
		workSetUncommittedWorks.clear();

		if ( workSetForcesRefresh ) {
			// In case of failure, just propagate the exception:
			// we don't expect a refresh failure to affect the writer.
			indexAccessor.refresh();
		}
	}

	private void cleanUpAfterFailure(Throwable throwable, Object failingOperation) {
		try {
			/*
			 * Note this will close the index writer,
			 * which with the default settings will trigger a commit.
			 */
			indexAccessor.reset();
		}
		catch (RuntimeException | IOException e) {
			throwable.addSuppressed( log.unableToCleanUpAfterError( eventContext, e ) );
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
		failureContextBuilder.indexName( indexName );
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
