/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.session;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * @deprecated Use {@link IndexingPlanSynchronizationStrategyConfigurationContext}
 */
@Deprecated(since = "6.2")
public interface AutomaticIndexingSynchronizationConfigurationContext {

	/**
	 * @param strategy A strategy describing how commits should be handled after document changes are applied.
	 * Defaults to {@link DocumentCommitStrategy#NONE}.
	 */
	void documentCommitStrategy(DocumentCommitStrategy strategy);

	/**
	 * @param strategy A strategy describing how refresh should be handled after document changes are applied.
	 * Defaults to {@link DocumentRefreshStrategy#NONE}.
	 */
	void documentRefreshStrategy(DocumentRefreshStrategy strategy);

	/**
	 * Set the handler for the (asynchronous) indexing future.
	 * <p>
	 * This typically involves waiting on the given future,
	 * to prevent the thread from resuming execution until indexing is complete.
	 *
	 * @param handler A handler that will be passed a future representing the progress of indexing.
	 * Defaults to a no-op handler.
	 * The future will be completed with an execution report once all document changes are applied.
	 * If any document change or the commit/refresh required by{@link #documentCommitStrategy(DocumentCommitStrategy)}
	 * and {@link #documentRefreshStrategy(DocumentRefreshStrategy)} failed,
	 * the report will {@link org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport#throwable() contain a throwable}
	 * and (if applicable) {@link org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport#failingEntities() a list of failing entities}.
	 */
	void indexingFutureHandler(
			Consumer<CompletableFuture<org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport>> handler);

	/**
	 * @return The failure handler.
	 * Use this to report failures that cannot be propagated by the {@link #indexingFutureHandler(Consumer)}.
	 */
	FailureHandler failureHandler();

	/**
	 * Set operation submitter to be applied while executing underlying plans.
	 *
	 * Using {@link OperationSubmitter#blocking()} by default.
	 *
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @see OperationSubmitter
	 */
	@Incubating
	void operationSubmitter(OperationSubmitter operationSubmitter);

}
