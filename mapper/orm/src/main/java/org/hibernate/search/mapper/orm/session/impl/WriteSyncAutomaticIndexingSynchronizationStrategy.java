/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class WriteSyncAutomaticIndexingSynchronizationStrategy
		implements AutomaticIndexingSynchronizationStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final AutomaticIndexingSynchronizationStrategy INSTANCE = new WriteSyncAutomaticIndexingSynchronizationStrategy();

	private WriteSyncAutomaticIndexingSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return AutomaticIndexingSynchronizationStrategy.class.getSimpleName() + ".writeSync()";
	}

	@Override
	public void apply(AutomaticIndexingSynchronizationConfigurationContext context) {
		// Request indexing to force a commit, but not necessarily a refresh.
		context.documentCommitStrategy( DocumentCommitStrategy.FORCE );
		context.documentRefreshStrategy( DocumentRefreshStrategy.NONE );
		context.indexingFutureHandler( future -> {
			// Wait for the result of indexing, so that we're sure changes were committed.
			SearchIndexingPlanExecutionReport report = Futures.unwrappedExceptionJoin( future );
			report.getThrowable().ifPresent( t -> {
				throw log.indexingFailure( t.getMessage(), report.getFailingEntities(), t );
			} );
		} );
	}
}
