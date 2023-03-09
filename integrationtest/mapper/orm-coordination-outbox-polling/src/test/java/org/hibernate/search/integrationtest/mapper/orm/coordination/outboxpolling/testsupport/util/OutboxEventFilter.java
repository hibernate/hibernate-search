/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventAndPredicate;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventOrder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventPredicate;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public class OutboxEventFilter {

	private volatile boolean filter = true;
	private final Set<UUID> allowedIds = ConcurrentHashMap.newKeySet();

	private DefaultOutboxEventFinder allEventsAllShardsFinder;
	private OutboxEventFinder visibleEventsAllShardsFinder;

	public OutboxEventFilter() {
	}

	public synchronized void reset() {
		filter = true;
		allowedIds.clear();
	}

	private synchronized void enableFilter(boolean enable) {
		filter = enable;
	}

	public OutboxEventFinderProvider wrap(DefaultOutboxEventFinder.Provider delegate) {
		allEventsAllShardsFinder = delegate.createWithoutStatusOrProcessAfterFilter();
		// This finder won't update `allowedIds`; that's on purpose.
		visibleEventsAllShardsFinder = delegate.create( Optional.of( new FilterById() ) );
		return new OutboxEventFinderProvider() {
			@Override
			public void appendTo(ToStringTreeBuilder builder) {
				builder.attribute( "filter", OutboxEventFilter.this );
				builder.attribute( "delegate", delegate );
			}

			@Override
			public OutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
				FilterById filterById = new FilterById();
				OutboxEventPredicate
						predicateWithFilter =
						predicate.<OutboxEventPredicate>map( p -> OutboxEventAndPredicate.of( p, filterById ) )
								.orElse( filterById );
				return new FilteringFinder( delegate.create( predicate ),
						delegate.create( Optional.of( predicateWithFilter ) ) );
			}
		};
	}

	public synchronized OutboxEventFilter showAllEvents() {
		// This is less costly memory-wise than adding "where e.id in (<here go 10,000 IDs>)" to the event finding query
		enableFilter( false );
		allowedIds.clear();
		return this;
	}

	public synchronized void showAllEventsUpToNow(SessionFactory sessionFactory) {
		enableFilter( true );
		with( sessionFactory ).runInTransaction( session -> showOnlyEvents( findOutboxEventIdsNoFilter( session ) ) );
	}

	public synchronized void showOnlyEvents(List<UUID> eventIds) {
		enableFilter( true );
		allowedIds.clear();
		allowedIds.addAll( eventIds );
	}

	public synchronized OutboxEventFilter hideAllEvents() {
		enableFilter( true );
		allowedIds.clear();
		return this;
	}

	public List<OutboxEvent> findOutboxEventsNoFilter(Session session) {
		return allEventsAllShardsFinder.createOutboxEventQuery( session )
				.getResultList();
	}

	public List<UUID> findOutboxEventIdsNoFilter(Session session) {
		return allEventsAllShardsFinder.createOutboxEventQueryForTests( session, alias -> alias + ".id", UUID.class,
						/* use the same order as the outbox event finder */ null )
				.getResultList();
	}

	public Long countOutboxEventsNoFilter(Session session) {
		return allEventsAllShardsFinder.createOutboxEventQueryForTests( session, alias -> "count(" + alias + ")", Long.class,
						/* ordering wouldn't make sense for a count() */ OutboxEventOrder.NONE )
				.getSingleResult();
	}

	private void checkFiltering() {
		if ( !filter ) {
			throw new IllegalStateException(
					"Cannot use filtering features while the filter is disabled; see enableFilter()" );
		}
	}

	public void awaitUntilNoMoreVisibleEvents(SessionFactory sessionFactory) {
		await().untilAsserted( () -> with( sessionFactory ).runInTransaction( session -> {
			List<OutboxEvent> outboxEntries = visibleEventsAllShardsFinder.findOutboxEvents( session, 1 );
			assertThat( outboxEntries ).isEmpty();
		} ) );
	}

	private class FilterById implements OutboxEventPredicate {
		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".id in :ids";
		}

		@Override
		public void setParams(Query<?> query) {
			// HSEARCH-4818: copy the values so that they don't change
			// between the binding and when the query actually executes.
			query.setParameter( "ids", new ArrayList<>( allowedIds ) );
		}
	}

	/**
	 * A replacement for the default outbox event finder that can prevent existing outbox events from being detected,
	 * thereby simulating a delay in the processing of outbox events.
	 */
	private class FilteringFinder implements OutboxEventFinder {
		private final OutboxEventFinder delegateWithoutFilter;
		private final OutboxEventFinder delegateWithFilter;

		private FilteringFinder(OutboxEventFinder delegateWithoutFilter, OutboxEventFinder delegateWithFilter) {
			this.delegateWithoutFilter = delegateWithoutFilter;
			this.delegateWithFilter = delegateWithFilter;
		}

		@Override
		public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
			synchronized ( OutboxEventFilter.this ) {
				OutboxEventFinder delegate = filter ? delegateWithFilter : delegateWithoutFilter;
				List<OutboxEvent> returned = delegate.findOutboxEvents( session, maxResults );
				// Only return each event once.
				// This is important because in the case of a retry, the same event will be reused.
				for ( OutboxEvent outboxEvent : returned ) {
					allowedIds.remove( outboxEvent.getId() );
				}
				return returned;
			}
		}
	}
}
