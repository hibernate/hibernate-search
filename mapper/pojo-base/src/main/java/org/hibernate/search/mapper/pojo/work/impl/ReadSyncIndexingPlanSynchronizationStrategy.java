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
public final class ReadSyncIndexingPlanSynchronizationStrategy implements IndexingPlanSynchronizationStrategy {

	public static final IndexingPlanSynchronizationStrategy INSTANCE = new ReadSyncIndexingPlanSynchronizationStrategy();

	private ReadSyncIndexingPlanSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return IndexingPlanSynchronizationStrategy.class.getSimpleName() + ".readSync()";
	}

	@Override
	public void apply(IndexingPlanSynchronizationStrategyConfigurationContext context) {
		// Request indexing to force a refresh, but not necessarily a commit.
		context.documentCommitStrategy( DocumentCommitStrategy.NONE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.FORCE );
		context.indexingFutureHandler( future -> {
			// Wait for the result of indexing, so that we're sure changes were applied and refreshed.
			SearchIndexingPlanExecutionReport report = Futures.unwrappedExceptionJoin( future );
			report.throwable().ifPresent( t -> {
				throw IndexingLog.INSTANCE.indexingFailure( t.getMessage(), report.failingEntities(), t );
			} );
		} );
	}
}
