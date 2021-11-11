/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.query.Query;

public final class DefaultOutboxEventFinder implements OutboxEventFinder {
	public static final class Provider implements OutboxEventFinderProvider {
		@Override
		public DefaultOutboxEventFinder create(Optional<OutboxEventPredicate> predicate) {
			return new DefaultOutboxEventFinder( predicate );
		}
	}

	public static String createQueryString(Optional<OutboxEventPredicate> predicate) {
		ProcessAfterFilter processAfterFilter = new ProcessAfterFilter();
		OutboxEventPredicate combined = ( predicate.isPresent() ) ?
				OutboxEventAndPredicate.of( predicate.get(), processAfterFilter ) :
				processAfterFilter;

		return "select e from OutboxEvent e where " + combined.queryPart( "e" ) + " order by e.id";
	}

	public static Query<OutboxEvent> createQuery(Session session, int maxResults,
			String queryString, Map<String, Object> params) {
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );

		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			query.setParameter( entry.getKey(), entry.getValue() );
		}
		query.setParameter( "now", Instant.now() );

		query.setMaxResults( maxResults );
		return query;
	}

	private final String queryString;
	private final Map<String, Object> params;

	public DefaultOutboxEventFinder(Optional<OutboxEventPredicate> predicate) {
		this.queryString = createQueryString( predicate );
		this.params = predicate.map( OutboxEventPredicate::params ).orElse( Collections.emptyMap() );
	}

	@Override
	public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		return DefaultOutboxEventFinder.createQuery( session, maxResults, queryString, params )
				.list();
	}

	private static class ProcessAfterFilter implements OutboxEventPredicate {
		@Override
		public String queryPart(String eventAlias) {
			return eventAlias + ".processAfter is null or " + eventAlias + ".processAfter < :now";
		}

		@Override
		public Map<String, Object> params() {
			return Collections.singletonMap( "now", Instant.now() );
		}
	}
}
