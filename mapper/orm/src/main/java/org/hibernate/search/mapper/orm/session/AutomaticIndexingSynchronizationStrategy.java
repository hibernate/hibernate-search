/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

/**
 * Determines how the thread will block upon committing a transaction
 * where indexed entities were modified.
 *
 * @see SearchSession#setAutomaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy)
 */
public interface AutomaticIndexingSynchronizationStrategy {

	void apply(AutomaticIndexingSynchronizationConfigurationContext context);

	/**
	 * @return A strategy that only waits for indexing requests to be queued in the backend.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy queued() {
		return context -> {
			context.documentCommitStrategy( DocumentCommitStrategy.NONE );
			context.documentCommitStrategy( DocumentCommitStrategy.NONE );
			context.indexingFutureHandler( future -> {
				// Nothing to do: once works are queued, we're done.
			} );
		};
	}

	/**
	 * @return A strategy that waits for indexing requests to be committed.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy committed() {
		return context -> {
			// Request indexing to force a commit, but not necessarily a refresh.
			context.documentCommitStrategy( DocumentCommitStrategy.FORCE );
			context.documentRefreshStrategy( DocumentRefreshStrategy.NONE );
			context.indexingFutureHandler( future -> {
				// Wait for the result of indexing, so that we're sure changes were committed.
				SearchIndexingPlanExecutionReport report = Futures.unwrappedExceptionJoin( future );
				report.getThrowable().ifPresent( t -> {
					throw Throwables.toRuntimeException( t );
				} );
			} );
		};
	}

	/**
	 * @return A strategy that waits for indexing requests to be committed and forces index refreshes.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy searchable() {
		return context -> {
			// Request indexing to force a commit and a refresh.
			context.documentCommitStrategy( DocumentCommitStrategy.FORCE );
			context.documentRefreshStrategy( DocumentRefreshStrategy.FORCE );
			context.indexingFutureHandler( future -> {
				// Wait for the result of indexing, so that we're sure changes were committed and refreshed.
				SearchIndexingPlanExecutionReport report = Futures.unwrappedExceptionJoin( future );
				report.getThrowable().ifPresent( t -> {
					throw Throwables.toRuntimeException( t );
				} );
			} );
		};
	}

}
