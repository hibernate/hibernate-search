/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
public class ConfiguredIndexingPlanSynchronizationStrategy {

	private final DocumentCommitStrategy documentCommitStrategy;
	private final DocumentRefreshStrategy documentRefreshStrategy;
	private final Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> indexingFutureHandler;
	private final OperationSubmitter operationSubmitter;

	protected ConfiguredIndexingPlanSynchronizationStrategy(Builder configurationContext) {
		this.documentCommitStrategy = configurationContext.documentCommitStrategy;
		this.documentRefreshStrategy = configurationContext.documentRefreshStrategy;
		this.indexingFutureHandler = configurationContext.indexingFutureHandler;
		this.operationSubmitter = configurationContext.operationSubmitter;
	}

	public DocumentCommitStrategy documentCommitStrategy() {
		return documentCommitStrategy;
	}

	public DocumentRefreshStrategy documentRefreshStrategy() {
		return documentRefreshStrategy;
	}

	public void executeAndSynchronize(PojoIndexingPlan indexingPlan) {
		CompletableFuture<SearchIndexingPlanExecutionReport> reportFuture =
				indexingPlan.executeAndReport( operationSubmitter )
						.thenApply( DelegatingSearchIndexingPlanExecutionReport::new );
		indexingFutureHandler.accept( reportFuture );
	}

	public static final class Builder
			implements IndexingPlanSynchronizationStrategyConfigurationContext {

		private final FailureHandler failureHandler;

		private DocumentCommitStrategy documentCommitStrategy = DocumentCommitStrategy.NONE;
		private DocumentRefreshStrategy documentRefreshStrategy = DocumentRefreshStrategy.NONE;
		private Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> indexingFutureHandler =
				future -> {};
		private OperationSubmitter operationSubmitter = OperationSubmitter.blocking();

		public Builder(FailureHandler failureHandler) {
			this.failureHandler = failureHandler;
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
		public void indexingFutureHandler(
				Consumer<? super CompletableFuture<? extends SearchIndexingPlanExecutionReport>> handler) {
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

		public ConfiguredIndexingPlanSynchronizationStrategy build() {
			return new ConfiguredIndexingPlanSynchronizationStrategy( this );
		}
	}

}
