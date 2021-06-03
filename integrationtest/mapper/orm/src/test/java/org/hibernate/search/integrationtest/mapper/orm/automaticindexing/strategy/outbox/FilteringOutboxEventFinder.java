/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.outbox.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEventFinder;

/**
 * A replacement for the default outbox event finder that can prevent existing outbox events from being detected,
 * thereby simulating a delay in the processing of outbox events.
 */
class FilteringOutboxEventFinder implements OutboxEventFinder {

	private final DefaultOutboxEventFinder defaultFinder = new DefaultOutboxEventFinder();

	private boolean filter = true;
	private final Set<Long> allowedIds = new HashSet<>();

	@Override
	public synchronized List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		if ( !filter ) {
			return defaultFinder.findOutboxEvents( session, maxResults );
		}

		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e where e.id in :ids order by e.id", OutboxEvent.class );
		query.setMaxResults( maxResults );
		query.setParameter( "ids", allowedIds );
		List<OutboxEvent> returned = query.list();
		// Only return each event once.
		// This is important because in the case of a retry, the same event will be reused.
		for ( OutboxEvent outboxEvent : returned ) {
			allowedIds.remove( outboxEvent.getId() );
		}
		return returned;
	}

	public synchronized FilteringOutboxEventFinder enableFilter(boolean enable) {
		filter = enable;
		return this;
	}

	public synchronized void showAllEventsUpToNow(SessionFactory sessionFactory) {
		checkFiltering();
		withinTransaction( sessionFactory, session -> showOnlyEvents( findOutboxEventIdsNoFilter( session ) ) );
	}

	public synchronized void showOnlyEvents(List<Long> eventIds) {
		checkFiltering();
		allowedIds.clear();
		allowedIds.addAll( eventIds );
	}

	public synchronized void hideAllEvents() {
		checkFiltering();
		allowedIds.clear();
	}

	public List<OutboxEvent> findOutboxEventsNoFilter(Session session) {
		checkFiltering();
		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e order by e.id", OutboxEvent.class );
		return query.list();
	}

	public List<Long> findOutboxEventIdsNoFilter(Session session) {
		checkFiltering();
		Query<Long> query = session.createQuery(
				"select e.id from OutboxEvent e order by e.id", Long.class );
		return query.list();
	}

	private void checkFiltering() {
		if ( !filter ) {
			throw new IllegalStateException(
					"Cannot use filtering features while the filter is disabled; see enableFilter()" );
		}
	}
}
