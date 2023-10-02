/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
