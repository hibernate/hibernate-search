/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.impl;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventFinderProvider;

/**
 * An internal configurer, used for tests.
 */
public interface OutboxPollingInternalConfigurer {

	OutboxEventFinderProvider wrapEventFinder(DefaultOutboxEventFinder.Provider delegate);

	AgentRepositoryProvider wrapAgentRepository(AgentRepositoryProvider delegate);

	OutboxPollingInternalConfigurer DEFAULT = new OutboxPollingInternalConfigurer() {
		public OutboxEventFinderProvider wrapEventFinder(DefaultOutboxEventFinder.Provider delegate) {
			return delegate;
		}

		public AgentRepositoryProvider wrapAgentRepository(AgentRepositoryProvider delegate) {
			return delegate;
		}
	};
}
