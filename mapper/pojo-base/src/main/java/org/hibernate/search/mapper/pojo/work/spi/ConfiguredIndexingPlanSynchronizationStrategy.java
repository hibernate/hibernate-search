/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.mapper.pojo.work.impl.DelegatingSearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Contracts;

@Incubating
public class ConfiguredIndexingPlanSynchronizationStrategy<E> {

	private final DocumentCommitStrategy documentCommitStrategy;
	private final DocumentRefreshStrategy documentRefreshStrategy;
	private final Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> indexingFutureHandler;
	private final OperationSubmitter operationSubmitter;
	private final EntityReferenceFactory<E> entityReferenceFactory;

	protected ConfiguredIndexingPlanSynchronizationStrategy(Builder<E> configurationContext) {
		this.documentCommitStrategy = configurationContext.documentCommitStrategy;
		this.documentRefreshStrategy = configurationContext.documentRefreshStrategy;
		this.indexingFutureHandler = configurationContext.indexingFutureHandler;
		this.operationSubmitter = configurationContext.operationSubmitter;
		this.entityReferenceFactory = configurationContext.entityReferenceFactory;
	}

	public DocumentCommitStrategy documentCommitStrategy() {
		return documentCommitStrategy;
	}

	public DocumentRefreshStrategy documentRefreshStrategy() {
		return documentRefreshStrategy;
	}

	public void executeAndSynchronize(PojoIndexingPlan indexingPlan) {
		CompletableFuture<SearchIndexingPlanExecutionReport> reportFuture =
				indexingPlan.executeAndReport( entityReferenceFactory, operationSubmitter )
						.thenApply( DelegatingSearchIndexingPlanExecutionReport::new );
		indexingFutureHandler.accept( reportFuture );
	}

	public static final class Builder<E>
			implements IndexingPlanSynchronizationStrategyConfigurationContext {

		private final FailureHandler failureHandler;

		private DocumentCommitStrategy documentCommitStrategy = DocumentCommitStrategy.NONE;
		private DocumentRefreshStrategy documentRefreshStrategy = DocumentRefreshStrategy.NONE;
		private Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> indexingFutureHandler = future -> { };
		private OperationSubmitter operationSubmitter = OperationSubmitter.blocking();

		private final EntityReferenceFactory<E> entityReferenceFactory;

		public Builder(FailureHandler failureHandler, EntityReferenceFactory<E> entityReferenceFactory) {
			this.failureHandler = failureHandler;
			this.entityReferenceFactory = entityReferenceFactory;
		}

		@Override
		public void documentCommitStrategy(DocumentCommitStrategy strategy) {
			Contracts.assertNotNull( strategy, "strategy" );
			this.documentCommitStrategy = strategy;
		}

		@Override
		public void documentRefreshStrategy(DocumentRefreshStrategy strategy) {
			Contracts.assertNotNull( strategy, "strategy" );
			this.documentRefreshStrategy = strategy;
		}

		@Override
		public void indexingFutureHandler(Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> handler) {
			Contracts.assertNotNull( handler, "handler" );
			this.indexingFutureHandler = handler;
		}

		@Override
		public FailureHandler failureHandler() {
			return failureHandler;
		}

		@Override
		public void operationSubmitter(OperationSubmitter operationSubmitter) {
			this.operationSubmitter = operationSubmitter;
		}

		public ConfiguredIndexingPlanSynchronizationStrategy<E> build() {
			return new ConfiguredIndexingPlanSynchronizationStrategy<>( this );
		}
	}

}
