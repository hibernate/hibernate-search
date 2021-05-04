/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

public final class DefaultOutboxEventFinder implements OutboxEventFinder {
	@Override
	public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e order by e.id", OutboxEvent.class );
		query.setMaxResults( maxResults );
		return query.list();
	}
}
