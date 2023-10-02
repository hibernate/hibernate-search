/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class DefaultOutboxEventFinder implements OutboxEventFinder {

	private static final OutboxEventAndPredicate BASE_PREDICATE_FILTER = OutboxEventAndPredicate.of(
			new ProcessAfterFilter(), new ProcessPendingFilter() );

	public static final class Provider extends OutboxEventFinderProvider {
		private final OutboxEventOrder order;

		public Provider(OutboxEventOrder order) {
			this.order = order;
		}

		@Override
		public void appendTo(ToStringTreeAppender appender) {
			appender.attribute( "order", order );
		}

		@Override
		public DefaultOutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
			OutboxEventPredicate combined = ( predicate.isPresent() )
					// Put the predicate first, because it's generally about sharding
					// and will greatly reduce the number of rows.
					? OutboxEventAndPredicate.of( predicate.get(), BASE_PREDICATE_FILTER )
					: BASE_PREDICATE_FILTER;
			return new DefaultOutboxEventFinder( Optional.of( combined ), order );
		}

		public DefaultOutboxEventFinder createWithoutStatusOrProcessAfterFilter() {
			return new DefaultOutboxEventFinder( Optional.empty(), order );
		}
	}

	private final String queryString;
	private final Optional<OutboxEventPredicate> predicate;
	private final OutboxEventOrder order;

	private DefaultOutboxEventFinder(Optional<OutboxEventPredicate> predicate, OutboxEventOrder order) {
		this.queryString = createQueryString( predicate, alias -> alias, order );
		this.predicate = predicate;
		this.order = order;
	}

	@Override
	public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		Query<OutboxEvent> query = createOutboxEventQuery( session );
		query.setMaxResults( maxResults );
		return query.list();
	}

	public Query<OutboxEvent> createOutboxEventQuery(Session session) {
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );
		if ( predicate.isPresent() ) {
			predicate.get().setParams( query );
		}
		return query;
	}

	public <T> Query<T> createOutboxEventQueryForTests(Session session,
			Function<String, String> selectClauseFunction, Class<T> resultType, OutboxEventOrder order) {
		String queryStringForTests = createQueryString( predicate, selectClauseFunction, order != null ? order : this.order );
		Query<T> query = session.createQuery( queryStringForTests, resultType );
		if ( predicate.isPresent() ) {
			predicate.get().setParams( query );
		}
		return query;
	}

	private String createQueryString(Optional<OutboxEventPredicate> predicate, Function<String, String> selectClauseFunction,
			OutboxEventOrder order) {
		return "select " + selectClauseFunction.apply( "e" )
				+ " from " + OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_NAME + " e "
				+ ( predicate.isPresent() ? " where " + predicate.get().queryPart( "e" ) : "" )
				+ order.queryPart( "e" );
	}

	private static class ProcessAfterFilter implements OutboxEventPredicate {

		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".processAfter < :now";
		}

		@Override
		public void setParams(Query<?> query) {
			query.setParameter( "now", Instant.now() );
		}
	}

	private static class ProcessPendingFilter implements OutboxEventPredicate {

		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".status = :status";
		}

		@Override
		public void setParams(Query<?> query) {
			query.setParameter( "status", OutboxEvent.Status.PENDING );
		}
	}
}
