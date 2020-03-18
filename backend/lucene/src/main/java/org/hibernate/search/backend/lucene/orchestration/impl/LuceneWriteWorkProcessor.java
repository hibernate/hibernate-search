/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * A thread-safe component responsible for applying write works to an index writer.
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
	public CompletableFuture<?> endBatch() {
		try {
			indexAccessor.commitOrDelay();
		}
		catch (RuntimeException e) {
			cleanUpAfterFailure( e, "Commit after a batch of index works" );
			// The exception was reported to the failure handler, no need to propagate it.
		}
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public long completeOrDelay() {
		try {
			return indexAccessor.commitOrDelay();
		}
		catch (RuntimeException e) {
			cleanUpAfterFailure( e, "Commit after completion of all remaining index works" );
			// The exception was reported to the failure handler, no need to propagate it.

			// Tell the executor there's no need to call us again later: the index writer was lost anyway.
			return 0;
		}
	}

	public <T> T submit(LuceneIndexManagementWork<T> work) {
		try {
			return work.execute( indexAccessor );
		}
		catch (RuntimeException e) {
			cleanUpAfterFailure( e, work.getInfo() );
			throw e;
		}
	}

	public <T> T submit(LuceneWriteWork<T> work) {
		try {
			return work.execute( context );
		}
		catch (RuntimeException e) {
			cleanUpAfterFailure( e, work.getInfo() );
			throw e;
		}
	}

	// Note this may be called outside of a batch
	public void forceCommit() {
		try {
			indexAccessor.commit();
		}
		catch (RuntimeException e) {
			cleanUpAfterFailure( e, "Commit after a set of index works" );
			throw e;
		}
	}

	// Note this may be called outside of a batch
	public void forceRefresh() {
		// In case of failure, just propagate the exception:
		// we don't expect a refresh failure to affect the writer and require a cleanup.
		indexAccessor.refresh();
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

		/*
		 * The failure will be reported elsewhere,
		 * but that report will not mention that some works from previous worksets may have been affected too.
		 * Report the failure again, just to warn about previous worksets potentially being affected.
		 */
		FailureContext.Builder failureContextBuilder = FailureContext.builder();
		failureContextBuilder.throwable( log.uncommittedOperationsBecauseOfFailure( indexName, throwable ) );
		failureContextBuilder.failingOperation( failingOperation );
		FailureContext failureContext = failureContextBuilder.build();
		failureHandler.handle( failureContext );
	}
}
