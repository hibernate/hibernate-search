/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.IndexingLog;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Futures;

@Incubating
public final class SyncIndexingPlanSynchronizationStrategy
		implements IndexingPlanSynchronizationStrategy {

	public static final IndexingPlanSynchronizationStrategy INSTANCE = new SyncIndexingPlanSynchronizationStrategy();

	private SyncIndexingPlanSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return IndexingPlanSynchronizationStrategy.class.getSimpleName() + ".sync()";
	}

	@Override
	public void apply(IndexingPlanSynchronizationStrategyConfigurationContext context) {
		// Request indexing to force a commit and a refresh.
		context.documentCommitStrategy( DocumentCommitStrategy.FORCE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.FORCE );
		context.indexingFutureHandler( future -> {
			// Wait for the result of indexing, so that we're sure changes were committed and refreshed.
			SearchIndexingPlanExecutionReport report = Futures.unwrappedExceptionJoin( future );
			report.throwable().ifPresent( t -> {
				throw IndexingLog.INSTANCE.indexingFailure( t.getMessage(), report.failingEntities(), t );
			} );
		} );
	}
}
