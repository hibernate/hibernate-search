/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.coordination.databasepolling.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.databasepolling.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.coordination.databasepolling.impl.OutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.databasepolling.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.databasepolling.impl.OutboxEventPredicate;

class FilteringOutboxEventFinder {

	private boolean filter = true;
	private final Set<Long> allowedIds = new HashSet<>();

	public FilteringOutboxEventFinder() {
	}

	public OutboxEventFinderProvider provider() {
		return new Provider();
	}

	public synchronized List<OutboxEvent> findOutboxEvents(Session session, int maxResults,
			Optional<OutboxEventPredicate> predicate) {
		Optional<OutboxEventPredicate> combinedPredicate = combineFilterWithPredicate( predicate );
		String queryString = DefaultOutboxEventFinder.createQueryString( combinedPredicate );
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );
		query.setMaxResults( maxResults );
		if ( combinedPredicate.isPresent() ) {
			for ( Map.Entry<String, Object> entry : combinedPredicate.get().params().entrySet() ) {
				query.setParameter( entry.getKey(), entry.getValue() );
			}
		}
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
				"select e from OutboxEvent e order by e.moment, e.id", OutboxEvent.class );
		return query.list();
	}

	public List<OutboxEvent> findOutboxEventsNoFilterById(Session session) {
		checkFiltering();
		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e order by e.id", OutboxEvent.class );
		return query.list();
	}

	public List<Long> findOutboxEventIdsNoFilter(Session session) {
		checkFiltering();
		Query<Long> query = session.createQuery(
				"select e.id from OutboxEvent e order by e.moment, e.id", Long.class );
		return query.list();
	}

	private void checkFiltering() {
		if ( !filter ) {
			throw new IllegalStateException(
					"Cannot use filtering features while the filter is disabled; see enableFilter()" );
		}
	}

	private Optional<OutboxEventPredicate> combineFilterWithPredicate(Optional<OutboxEventPredicate> predicate) {
		if ( !filter ) {
			return predicate;
		}

		OutboxEventPredicate filterPredicate = new OutboxEventPredicate() {
			@Override
			public String queryPart(String eventAlias) {
				return eventAlias + ".id in :ids";
			}

			@Override
			public Map<String, Object> params() {
				return Collections.singletonMap( "ids", allowedIds );
			}
		};

		if ( !predicate.isPresent() ) {
			return Optional.of( filterPredicate );
		}

		// Need to combine the predicates...
		return Optional.of( and( predicate.get(), filterPredicate ) );
	}

	private OutboxEventPredicate and(OutboxEventPredicate left, OutboxEventPredicate right) {
		return new OutboxEventPredicate() {
			@Override
			public String queryPart(String eventAlias) {
				return "(" + left.queryPart( eventAlias ) + ") and (" + right.queryPart( eventAlias ) + ")";
			}

			@Override
			public Map<String, Object> params() {
				Map<String, Object> merged = new HashMap<>();
				// Assuming no conflicts...
				merged.putAll( left.params() );
				merged.putAll( right.params() );
				return merged;
			}
		};
	}

	/**
	 * A replacement for the default outbox event finder that can prevent existing outbox events from being detected,
	 * thereby simulating a delay in the processing of outbox events.
	 */
	private class Provider implements OutboxEventFinderProvider {
		@Override
		public OutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
			return (session, maxResults) -> FilteringOutboxEventFinder.this.findOutboxEvents(
					session, maxResults, predicate );
		}
	}
}
