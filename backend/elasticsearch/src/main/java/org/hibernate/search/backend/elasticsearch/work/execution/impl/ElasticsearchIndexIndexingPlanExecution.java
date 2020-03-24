/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;

/**
 * A single-use, stateful execution of a set of works as part of an indexing plan.
 *
 * @param <R> The type of entity references in the {@link #execute() execution report}.
 */
class ElasticsearchIndexIndexingPlanExecution<R> {

	private final ElasticsearchSerialWorkOrchestrator orchestrator;
	private final EntityReferenceFactory<R> entityReferenceFactory;

	private final List<SingleDocumentWork> works;
	private final CompletableFuture<Void>[] futures;

	ElasticsearchIndexIndexingPlanExecution(ElasticsearchSerialWorkOrchestrator orchestrator,
			EntityReferenceFactory<R> entityReferenceFactory,
			List<SingleDocumentWork> works) {
		this.orchestrator = orchestrator;
		this.entityReferenceFactory = entityReferenceFactory;
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
	CompletableFuture<IndexIndexingPlanExecutionReport<R>> execute() {
		// Add the handler to the future *before* submitting the works,
		// so as to be sure that onAllWorksFinished is executed in the background,
		// not in the current thread.
		CompletableFuture<IndexIndexingPlanExecutionReport<R>> reportFuture = CompletableFuture.allOf( futures )
				// We don't care about the throwable, as it comes from a work and
				// work failures are handled in onAllWorksFinished
				.handle( (result, throwable) -> onAllWorksFinished() );

		for ( int i = 0; i < works.size(); i++ ) {
			CompletableFuture<Void> future = futures[i];
			SingleDocumentWork work = works.get( i );
			orchestrator.submit( future, work );
		}

		return reportFuture;
	}

	private IndexIndexingPlanExecutionReport<R> onAllWorksFinished() {
		return buildReport();
	}

	private IndexIndexingPlanExecutionReport<R> buildReport() {
		IndexIndexingPlanExecutionReport.Builder<R> reportBuilder = IndexIndexingPlanExecutionReport.builder();
		for ( int i = 0; i < futures.length; i++ ) {
			CompletableFuture<?> future = futures[i];
			if ( future.isCompletedExceptionally() ) {
				reportBuilder.throwable( Futures.getThrowableNow( future ) );
				SingleDocumentWork work = works.get( i );
				try {
					reportBuilder.failingEntityReference(
							entityReferenceFactory,
							work.getEntityTypeName(), work.getEntityIdentifier()
					);
				}
				catch (RuntimeException e) {
					reportBuilder.throwable( e );
				}
			}
		}
		return reportBuilder.build();
	}

}
