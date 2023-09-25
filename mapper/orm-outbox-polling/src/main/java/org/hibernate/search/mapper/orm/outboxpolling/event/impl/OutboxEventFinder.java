/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.List;

import org.hibernate.Session;

public interface OutboxEventFinder {
	List<OutboxEvent> findOutboxEvents(Session session, int maxResults);
}
