/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.plan.synchronization.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.plan.synchronization.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ReadSyncIndexingPlanSynchronizationStrategy<E> implements IndexingPlanSynchronizationStrategy<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ReadSyncIndexingPlanSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return IndexingPlanSynchronizationStrategy.class.getSimpleName() + ".readSync()";
	}

	@Override
	public void apply(IndexingPlanSynchronizationStrategyConfigurationContext<E> context) {
		// Request indexing to force a refresh, but not necessarily a commit.
		context.documentCommitStrategy( DocumentCommitStrategy.NONE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.FORCE );
		context.indexingFutureHandler( future -> {
			// Wait for the result of indexing, so that we're sure changes were applied and refreshed.
			SearchIndexingPlanExecutionReport<E> report = Futures.unwrappedExceptionJoin( future );
			report.throwable().ifPresent( t -> {
				throw log.indexingFailure( t.getMessage(), report.failingEntities(), t );
			} );
		} );
	}
}
