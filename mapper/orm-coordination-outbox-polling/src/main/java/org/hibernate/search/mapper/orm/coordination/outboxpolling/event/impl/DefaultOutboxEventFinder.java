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
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public final class DefaultOutboxEventFinder implements OutboxEventFinder {

	private static final OutboxEventAndPredicate BASE_PREDICATE_FILTER = OutboxEventAndPredicate.of(
			new ProcessAfterFilter(), new ProcessPendingFilter() );

	public static final class Provider extends OutboxEventFinderProvider {
		@Override
		public void appendTo(ToStringTreeBuilder builder) {
			// Nothing to display here
		}

		@Override
		public DefaultOutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
			OutboxEventPredicate combined = ( predicate.isPresent() )
					? OutboxEventAndPredicate.of( predicate.get(), BASE_PREDICATE_FILTER )
					: BASE_PREDICATE_FILTER;
			return new DefaultOutboxEventFinder( Optional.of( combined ) );
		}

		public DefaultOutboxEventFinder createWithoutStatusOrProcessAfterFilter() {
			return new DefaultOutboxEventFinder( Optional.empty() );
		}
	}

	private final String queryString;
	private final Optional<OutboxEventPredicate> predicate;

	private DefaultOutboxEventFinder(Optional<OutboxEventPredicate> predicate) {
		this.queryString = "select e from " + ENTITY_NAME + " e "
				+ ( predicate.isPresent() ? " where " + predicate.get().queryPart( "e" ) : "" )
				+ " order by e.created, e.id";
		this.predicate = predicate;
	}

	@Override
	public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );
		if ( predicate.isPresent() ) {
			predicate.get().setParams( query );
		}

		query.setMaxResults( maxResults );

		return query.list();
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
