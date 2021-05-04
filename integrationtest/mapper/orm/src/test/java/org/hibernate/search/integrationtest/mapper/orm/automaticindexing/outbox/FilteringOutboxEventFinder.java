/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.outbox;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEventFinder;

/**
 * A replacement for the default outbox event finder that can prevent existing outbox events from being detected,
 * thereby simulating a delay in the processing of outbox events.
 */
class FilteringOutboxEventFinder implements OutboxEventFinder {
	private final Set<Integer> allowedIds = new HashSet<>();

	@Override
	public synchronized List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e where e.id in :ids order by e.id", OutboxEvent.class );
		query.setMaxResults( maxResults );
		query.setParameter( "ids", allowedIds );
		return query.list();
	}

	public synchronized void showAllEventsUpToNow(SessionFactory sessionFactory) {
		withinTransaction( sessionFactory, session -> showOnlyEvents( findOutboxEventIdsNoFilter( session ) ) );
	}

	public synchronized void showOnlyEvents(List<Integer> eventIds) {
		allowedIds.clear();
		allowedIds.addAll( eventIds );
	}

	public synchronized void hideAllEvents() {
		allowedIds.clear();
	}

	public List<OutboxEvent> findOutboxEventsNoFilter(Session session) {
		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e order by e.id", OutboxEvent.class );
		return query.list();
	}

	public List<Integer> findOutboxEventIdsNoFilter(Session session) {
		Query<Integer> query = session.createQuery(
				"select e.id from OutboxEvent e order by e.id", Integer.class );
		return query.list();
	}
}
