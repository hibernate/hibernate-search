/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyConfigurationContext;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public final class WriteSyncIndexingPlanSynchronizationStrategy implements IndexingPlanSynchronizationStrategy {

	public static final IndexingPlanSynchronizationStrategy INSTANCE = new WriteSyncIndexingPlanSynchronizationStrategy();
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private WriteSyncIndexingPlanSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return IndexingPlanSynchronizationStrategy.class.getSimpleName() + ".writeSync()";
	}

	@Override
	public void apply(IndexingPlanSynchronizationStrategyConfigurationContext context) {
		// Request indexing to force a commit, but not necessarily a refresh.
		context.documentCommitStrategy( DocumentCommitStrategy.FORCE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.NONE );
		context.indexingFutureHandler( future -> {
			// Wait for the result of indexing, so that we're sure changes were committed.
			SearchIndexingPlanExecutionReport report = Futures.unwrappedExceptionJoin( future );
			report.throwable().ifPresent( t -> {
				throw log.indexingFailure( t.getMessage(), report.failingEntities(), t );
			} );
		} );
	}
}
