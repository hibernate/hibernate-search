/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.impl.Futures;

/**
 * A single-use, stateful execution of a set of works as part of an indexing plan.
 *
 * @param <R> The type of entity references in the {@link #execute() execution report}.
 */
class LuceneIndexIndexingPlanExecution<R> {

	private final LuceneSerialWorkOrchestrator orchestrator;
	private final EntityReferenceFactory<R> entityReferenceFactory;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final List<SingleDocumentIndexingWork> works;
	private final CompletableFuture<Long>[] futures;

	@SuppressWarnings("unchecked")
	LuceneIndexIndexingPlanExecution(LuceneSerialWorkOrchestrator orchestrator,
			EntityReferenceFactory<R> entityReferenceFactory,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy,
			List<SingleDocumentIndexingWork> works) {
		this.orchestrator = orchestrator;
		this.entityReferenceFactory = entityReferenceFactory;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
		this.works = works;
		this.futures = new CompletableFuture[works.size()];
		for ( int i = 0; i < futures.length; i++ ) {
			futures[i] = new CompletableFuture<>();
		}
	}

	/**
	 * Submits the works to the orchestrators for execution,
	 * planning all necessary post-execution operations (commit, refresh) as appropriate.
	 * <p>
	 * Must only be called once.
	 *
	 * @return A future that completes when all works and optionally commit/refresh have completed,
	 * holding an execution report.
	 */
	CompletableFuture<MultiEntityOperationExecutionReport<R>> execute(OperationSubmitter operationSubmitter) {
		// Add the handler to the future *before* submitting the works,
		// so as to be sure that onAllWorksFinished is executed in the background,
		// not in the current thread.
		// It's important because we don't want to block the current thread.
		CompletableFuture<MultiEntityOperationExecutionReport<R>> reportFuture = CompletableFuture.allOf( futures )
				// We don't care about the throwable, as it comes from a work and
				// work failures are handled in onAllWorksFinished
				.handle( (result, throwable) -> onAllWorksFinished() );

		for ( int i = 0; i < works.size(); i++ ) {
			CompletableFuture<Long> future = futures[i];
			SingleDocumentIndexingWork work = works.get( i );
			orchestrator.submit( future, work, operationSubmitter );
		}

		return reportFuture;
	}

	private MultiEntityOperationExecutionReport<R> onAllWorksFinished() {
		Throwable commitOrRefreshThrowable = null;
		try {
			commitOrRefreshAsNecessary();
		}
		catch (RuntimeException e) {
			commitOrRefreshThrowable = e;
		}

		return buildReport( commitOrRefreshThrowable );
	}

	private void commitOrRefreshAsNecessary() {
		if ( DocumentCommitStrategy.FORCE == commitStrategy ) {
			// In case of failure, just propagate the exception.
			// We'll skip the refresh, but that's okay: the underlying writers/readers are reset anyway.
			orchestrator.forceCommitInCurrentThread();
		}

		if ( DocumentRefreshStrategy.FORCE == refreshStrategy ) {
			orchestrator.forceRefreshInCurrentThread();
		}
	}

	private MultiEntityOperationExecutionReport<R> buildReport(Throwable commitOrRefreshThrowable) {
		MultiEntityOperationExecutionReport.Builder<R> reportBuilder = MultiEntityOperationExecutionReport.builder();
		for ( int i = 0; i < futures.length; i++ ) {
			CompletableFuture<?> future = futures[i];
			if ( future.isCompletedExceptionally() ) {
				reportBuilder.throwable( Futures.getThrowableNow( future ) );
			}
			if ( commitOrRefreshThrowable != null || future.isCompletedExceptionally() ) {
				SingleDocumentIndexingWork work = works.get( i );
				reportBuilder.failingEntityReference( entityReferenceFactory, work.getEntityTypeName(),
						work.getEntityIdentifier() );
			}
		}
		if ( commitOrRefreshThrowable != null ) {
			reportBuilder.throwable( commitOrRefreshThrowable );
		}
		return reportBuilder.build();
	}

}
