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
import org.hibernate.search.backend.elasticsearch.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.impl.Futures;

/**
 * A single-use, stateful execution of a set of works as part of an indexing plan.
 */
class ElasticsearchIndexIndexingPlanExecution {

	private final ElasticsearchSerialWorkOrchestrator orchestrator;
	private final EntityReferenceFactory entityReferenceFactory;

	private final List<SingleDocumentIndexingWork> works;
	private final CompletableFuture<Void>[] futures;

	@SuppressWarnings("unchecked")
	ElasticsearchIndexIndexingPlanExecution(ElasticsearchSerialWorkOrchestrator orchestrator,
			EntityReferenceFactory entityReferenceFactory,
			List<SingleDocumentIndexingWork> works) {
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
	CompletableFuture<MultiEntityOperationExecutionReport> execute(OperationSubmitter operationSubmitter) {
		// Add the handler to the future *before* submitting the works,
		// so as to be sure that onAllWorksFinished is executed in the background,
		// not in the current thread.
		CompletableFuture<MultiEntityOperationExecutionReport> reportFuture = CompletableFuture.allOf( futures )
				// We don't care about the throwable, as it comes from a work and
				// work failures are handled in onAllWorksFinished
				.handle( (result, throwable) -> onAllWorksFinished() );

		for ( int i = 0; i < works.size(); i++ ) {
			CompletableFuture<Void> future = futures[i];
			SingleDocumentIndexingWork work = works.get( i );
			orchestrator.submit( future, work, operationSubmitter );
		}

		return reportFuture;
	}

	private MultiEntityOperationExecutionReport onAllWorksFinished() {
		return buildReport();
	}

	private MultiEntityOperationExecutionReport buildReport() {
		MultiEntityOperationExecutionReport.Builder reportBuilder = MultiEntityOperationExecutionReport.builder();
		for ( int i = 0; i < futures.length; i++ ) {
			CompletableFuture<?> future = futures[i];
			if ( future.isCompletedExceptionally() ) {
				reportBuilder.throwable( Futures.getThrowableNow( future ) );
				SingleDocumentIndexingWork work = works.get( i );
				reportBuilder.failingEntityReference( entityReferenceFactory, work.getEntityTypeName(),
						work.getEntityIdentifier() );
			}
		}
		return reportBuilder.build();
	}

}
