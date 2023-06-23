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
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;

@SuppressWarnings("deprecation")
public class DelegatingAutomaticIndexingSynchronizationStrategy
		implements org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy {

	private final IndexingPlanSynchronizationStrategy delegate;

	public DelegatingAutomaticIndexingSynchronizationStrategy(IndexingPlanSynchronizationStrategy delegate) {
		this.delegate = delegate;
	}

	@Override
	public void apply(
			org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationConfigurationContext context) {
		delegate.apply( new IndexingPlanSynchronizationStrategyConfigurationContext() {
			@Override
			public void documentCommitStrategy(DocumentCommitStrategy strategy) {
				context.documentCommitStrategy( strategy );
			}

			@Override
			public void documentRefreshStrategy(DocumentRefreshStrategy strategy) {
				context.documentRefreshStrategy( strategy );
			}

			@Override
			public void indexingFutureHandler(
					Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> handler) {
				context.indexingFutureHandler(
						report -> handler
								.accept( report.thenApply( HibernateOrmDelegatingSearchIndexingPlanExecutionReport::new ) )
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

	public IndexingPlanSynchronizationStrategy delegate() {
		return delegate;
	}

	private static class HibernateOrmDelegatingSearchIndexingPlanExecutionReport implements SearchIndexingPlanExecutionReport {

		private final org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport report;

		private HibernateOrmDelegatingSearchIndexingPlanExecutionReport(
				org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport report) {
			this.report = report;
		}

		@Override
		public Optional<Throwable> throwable() {
			return report.throwable();
		}

		@Override
		public List<? extends EntityReference> failingEntities() {
			return report.failingEntities();
		}
	}
}
