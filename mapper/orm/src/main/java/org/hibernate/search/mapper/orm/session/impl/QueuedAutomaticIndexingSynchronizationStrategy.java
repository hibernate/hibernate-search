/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationConfigurationContext;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;

public final class QueuedAutomaticIndexingSynchronizationStrategy
		implements AutomaticIndexingSynchronizationStrategy {
	public static final AutomaticIndexingSynchronizationStrategy INSTANCE = new QueuedAutomaticIndexingSynchronizationStrategy();

	private QueuedAutomaticIndexingSynchronizationStrategy() {
	}

	@Override
	public String toString() {
		return AutomaticIndexingSynchronizationStrategy.class.getSimpleName() + ".queued()";
	}

	@Override
	public void apply(AutomaticIndexingSynchronizationConfigurationContext context) {
		context.documentCommitStrategy( DocumentCommitStrategy.NONE );
		context.documentCommitStrategy( DocumentCommitStrategy.NONE );
		context.indexingFutureHandler( future -> {
			// Nothing to do: once works are queued, we're done.
		} );
	}
}
