/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.session.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.session.HibernateOrmIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;

@SuppressWarnings( "deprecation" )
public class DelegatingAutomaticIndexingSynchronizationStrategy implements org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy {

	private final HibernateOrmIndexingPlanSynchronizationStrategy delegate;

	public DelegatingAutomaticIndexingSynchronizationStrategy(HibernateOrmIndexingPlanSynchronizationStrategy delegate) {
		this.delegate = delegate;
	}

	@Override
	public void apply(org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationConfigurationContext context) {
		delegate.apply( new IndexingPlanSynchronizationStrategyConfigurationContext<EntityReference>() {
			@Override
			public void documentCommitStrategy(DocumentCommitStrategy strategy) {
				context.documentCommitStrategy( strategy );
			}

			@Override
			public void documentRefreshStrategy(DocumentRefreshStrategy strategy) {
				context.documentRefreshStrategy( strategy );
			}

			@Override
			public void indexingFutureHandler(Consumer<CompletableFuture<SearchIndexingPlanExecutionReport<EntityReference>>> handler) {
				context.indexingFutureHandler( report ->
						handler.accept( report.thenApply( DelegatingSearchIndexingPlanExecutionReport::new ) )
				);
			}

			@Override
			public FailureHandler failureHandler() {
				return context.failureHandler();
			}

			@Override
			public void operationSubmitter(OperationSubmitter operationSubmitter) {
				context.operationSubmitter( operationSubmitter );
			}
		} );
	}

	public HibernateOrmIndexingPlanSynchronizationStrategy delegate() {
		return delegate;
	}

	private static class DelegatingSearchIndexingPlanExecutionReport implements SearchIndexingPlanExecutionReport<EntityReference> {

		private final org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport report;

		private DelegatingSearchIndexingPlanExecutionReport(org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport report) {
			this.report = report;
		}

		@Override
		public Optional<Throwable> throwable() {
			return report.throwable();
		}

		@Override
		public List<EntityReference> failingEntities() {
			return report.failingEntities();
		}
	}
}
