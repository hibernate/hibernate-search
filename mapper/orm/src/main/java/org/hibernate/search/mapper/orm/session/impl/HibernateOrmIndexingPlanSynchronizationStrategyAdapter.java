/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;

@SuppressWarnings("deprecation")
public class HibernateOrmIndexingPlanSynchronizationStrategyAdapter implements IndexingPlanSynchronizationStrategy {

	private final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy delegate;

	public HibernateOrmIndexingPlanSynchronizationStrategyAdapter(
			org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy delegate) {
		this.delegate = delegate;
	}

	@Override
	public void apply(IndexingPlanSynchronizationStrategyConfigurationContext context) {
		delegate.apply(
				new org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationConfigurationContext() {
					@Override
					public void documentCommitStrategy(DocumentCommitStrategy strategy) {
						context.documentCommitStrategy( strategy );
					}

					@Override
					public void documentRefreshStrategy(DocumentRefreshStrategy strategy) {
						context.documentRefreshStrategy( strategy );
					}

					@Override
					@SuppressWarnings("deprecation") // need to keep OLD API still implemented
					public void indexingFutureHandler(
							Consumer<CompletableFuture<org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport>> handler) {
						context.indexingFutureHandler( report -> handler.accept(
								report.thenApply( r ->
										new org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport() {
											@Override
											public Optional<Throwable> throwable() {
												return r.throwable();
											}

											@Override
											public List<EntityReference> failingEntities() {
												return r.failingEntities().stream()
														.map( EntityReference.class::cast )
														.collect( Collectors.toList() );
											}
										}
								) )
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
				}
		);
	}

	@Override
	public String toString() {
		return HibernateOrmIndexingPlanSynchronizationStrategyAdapter.class.getSimpleName() + "(" + delegate.toString() + ")";
	}
}
