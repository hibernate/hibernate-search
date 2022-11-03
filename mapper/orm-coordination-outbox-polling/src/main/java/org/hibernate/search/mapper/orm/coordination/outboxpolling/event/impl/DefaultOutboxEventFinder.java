/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_NAME;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.query.Query;

public final class DefaultOutboxEventFinder implements OutboxEventFinder {

	private static OutboxEventAndPredicate basePredicateFilter = OutboxEventAndPredicate.of(
			new ProcessAfterFilter(), new ProcessPendingFilter() );

	public static final class Provider implements OutboxEventFinderProvider {
		@Override
		public DefaultOutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
			return new DefaultOutboxEventFinder( predicate );
		}
	}

	public static String createQueryString(Optional<OutboxEventPredicate> predicate) {
		OutboxEventPredicate combined = ( predicate.isPresent() ) ?
				OutboxEventAndPredicate.of( predicate.get(), basePredicateFilter ) :
				basePredicateFilter;

		return "select e from " + ENTITY_NAME + " e where " + combined.queryPart( "e" ) + " order by e.created, e.id";
	}

	public static Query<OutboxEvent> createQuery(Session session, int maxResults,
			String queryString, Optional<OutboxEventPredicate> predicate) {
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );
		if ( predicate.isPresent() ) {
			predicate.get().setParams( query );
		}
		basePredicateFilter.setParams( query );

		query.setMaxResults( maxResults );
		return query;
	}

	private final String queryString;
	private final Optional<OutboxEventPredicate> predicate;

	public DefaultOutboxEventFinder(Optional<OutboxEventPredicate> predicate) {
		this.queryString = createQueryString( predicate );
		this.predicate = predicate;
	}

	@Override
	public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		return DefaultOutboxEventFinder.createQuery( session, maxResults, queryString, predicate )
				.list();
	}

	private static class ProcessAfterFilter implements OutboxEventPredicate {

		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".processAfter is null or " + eventAlias + ".processAfter < :now";
		}

		@Override
		public void setParams(Query<OutboxEvent> query) {
			query.setParameter( "now", Instant.now() );
		}
	}

	private static class ProcessPendingFilter implements OutboxEventPredicate {

		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".status = :status";
		}

		@Override
		public void setParams(Query<OutboxEvent> query) {
			query.setParameter( "status", OutboxEvent.Status.PENDING );
		}
	}
}
