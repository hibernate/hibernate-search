/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.List;

import org.hibernate.Session;

public interface OutboxEventFinder {
	List<OutboxEvent> findOutboxEvents(Session session, int maxResults);
}
