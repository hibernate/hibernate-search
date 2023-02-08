/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.plan.synchronization.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.mapper.pojo.work.impl.SearchIndexingPlanExecutionReportImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Contracts;

public class ConfiguredIndexingPlanSynchronizationStrategy<E> {

	private final DocumentCommitStrategy documentCommitStrategy;
	private final DocumentRefreshStrategy documentRefreshStrategy;
	private final Consumer<CompletableFuture<SearchIndexingPlanExecutionReport>> indexingFutureHandler;
	private final OperationSubmitter operationSubmitter;
	private final EntityReferenceFactory<E> entityReferenceFactory;

	protected ConfiguredIndexingPlanSynchronizationStrategy(Builder<E> configurationContext) {
		this.documentCommitStrategy = configurationContext.documentCommitStrategy;
		this.documentRefreshStrategy = configurationContext.documentRefreshStrategy;
		this.indexingFutureHandler = configurationContext.indexingFutureHandler;
		this.operationSubmitter = configurationContext.operationSubmitter;
		this.entityReferenceFactory = configurationContext.entityReferenceFactory;
	}

	public DocumentCommitStrategy getDocumentCommitStrategy() {
		return documentCommitStrategy;
	}

	public DocumentRefreshStrategy getDocumentRefreshStrategy() {
		return documentRefreshStrategy;
	}

	public void executeAndSynchronize(PojoIndexingPlan indexingPlan) {
		CompletableFuture<SearchIndexingPlanExecutionReport> reportFuture =
				indexingPlan.executeAndReport( entityReferenceFactory, operationSubmitter )
						.thenApply( SearchIndexingPlanExecutionReportImpl::new );
		indexingFutureHandler.accept( reportFuture );
	}

	public static final class Builder<E>
			implements IndexingPlanSynchronizationStrategyConfigurationContext {

		private final FailureHandler failureHandler;

		private DocumentCommitStrategy documentCommitStrategy = DocumentCommitStrategy.NONE;
		private DocumentRefreshStrategy documentRefreshStrategy = DocumentRefreshStrategy.NONE;
		private Consumer<CompletableFuture<SearchIndexingPlanExecutionReport>> indexingFutureHandler = future -> {
		};
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
		public void indexingFutureHandler(Consumer<CompletableFuture<SearchIndexingPlanExecutionReport>> handler) {
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
