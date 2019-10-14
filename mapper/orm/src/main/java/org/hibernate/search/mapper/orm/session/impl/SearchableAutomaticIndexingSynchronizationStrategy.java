/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlanExecutionReport;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

public final class SearchableAutomaticIndexingSynchronizationStrategy
		implements AutomaticIndexingSynchronizationStrategy {
	public static final AutomaticIndexingSynchronizationStrategy INSTANCE = new SearchableAutomaticIndexingSynchronizationStrategy();

	private SearchableAutomaticIndexingSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return AutomaticIndexingSynchronizationStrategy.class.getSimpleName() + ".searchable()";
	}

	@Override
	public void apply(AutomaticIndexingSynchronizationConfigurationContext context) {
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
	}
}
