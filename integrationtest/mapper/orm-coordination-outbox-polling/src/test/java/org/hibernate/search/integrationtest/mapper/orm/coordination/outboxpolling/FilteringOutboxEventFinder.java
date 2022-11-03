/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_NAME;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventAndPredicate;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventPredicate;

public class FilteringOutboxEventFinder {

	private volatile boolean filter = true;
	private final Set<UUID> allowedIds = ConcurrentHashMap.newKeySet();

	public FilteringOutboxEventFinder() {
	}

	public synchronized void reset() {
		filter = true;
		allowedIds.clear();
	}

	public OutboxEventFinderProvider provider() {
		return new Provider();
	}

	public synchronized List<OutboxEvent> findOutboxEvents(Session session, int maxResults,
			Optional<OutboxEventPredicate> predicate) {
		Query<OutboxEvent> query = createQuery( session, maxResults, predicate );
		List<OutboxEvent> returned = query.list();
		// Only return each event once.
		// This is important because in the case of a retry, the same event will be reused.
		for ( OutboxEvent outboxEvent : returned ) {
			allowedIds.remove( outboxEvent.getId() );
		}
		return returned;
	}

	// Find outbox events as shown by the filter, but not for processing:
	// don't update the filter as a result of this query.
	public synchronized List<OutboxEvent> findOutboxEventsNotForProcessing(Session session, int maxResults,
			Optional<OutboxEventPredicate> predicate) {
		Query<OutboxEvent> query = createQuery( session, maxResults, predicate );
		return query.list();
	}

	private Query<OutboxEvent> createQuery(Session session, int maxResults,
			Optional<OutboxEventPredicate> predicate) {
		Optional<OutboxEventPredicate> combinedPredicate = combineFilterWithPredicate( predicate );
		String queryString = DefaultOutboxEventFinder.createQueryString( combinedPredicate );
		Query<OutboxEvent> query = DefaultOutboxEventFinder.createQuery( session, maxResults, queryString,
				combinedPredicate );
		avoidLockingConflicts( query );
		return query;
	}

	public synchronized FilteringOutboxEventFinder enableFilter(boolean enable) {
		filter = enable;
		return this;
	}

	public synchronized void showAllEventsUpToNow(SessionFactory sessionFactory) {
		checkFiltering();
		with( sessionFactory ).runInTransaction( session -> showOnlyEvents( findOutboxEventIdsNoFilter( session ) ) );
	}

	public synchronized void showOnlyEvents(List<UUID> eventIds) {
		checkFiltering();
		allowedIds.clear();
		allowedIds.addAll( eventIds );
	}

	public synchronized void hideAllEvents() {
		checkFiltering();
		allowedIds.clear();
	}

	// Orders events by ID, regardless of what order is used when processing them.
	public List<OutboxEvent> findOutboxEventsNoFilter(Session session) {
		checkFiltering();
		Query<OutboxEvent> query = session.createQuery(
				"select e from " + ENTITY_NAME + " e order by e.created, e.id", OutboxEvent.class );
		avoidLockingConflicts( query );
		return query.list();
	}

	public List<UUID> findOutboxEventIdsNoFilter(Session session) {
		checkFiltering();
		Query<UUID> query = session.createQuery(
				"select e.id from " + ENTITY_NAME + " e order by e.created, e.id", UUID.class );
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

		FilterById filterById = new FilterById();
		if ( !predicate.isPresent() ) {
			return Optional.of( filterById );
		}

		// Need to combine the predicates...
		return Optional.of( OutboxEventAndPredicate.of( predicate.get(), filterById ) );
	}

	// Configures a query to avoid locking on events,
	// so as not to conflict with background processors.
	private void avoidLockingConflicts(Query<OutboxEvent> query) {
		query.setLockOptions( LockOptions.NONE );
	}

	public void awaitUntilNoMoreVisibleEvents(SessionFactory sessionFactory) {
		await().untilAsserted( () -> with( sessionFactory ).runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = findOutboxEventsNotForProcessing( session, 1, Optional.empty() );
			assertThat( outboxEntries ).isEmpty();
		} ) );
	}

	private class FilterById implements OutboxEventPredicate {
		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".id in :ids";
		}

		@Override
		public void setParams(Query<OutboxEvent> query) {
			query.setParameter( "ids", allowedIds );
		}
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
