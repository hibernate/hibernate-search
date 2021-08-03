/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.LockModeType;

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
		return "select e from OutboxEvent e"
				+ predicate.map( p -> " where " + p.queryPart( "e" ) ).orElse( "" )
				+ " order by e.moment, e.id";
	}

	public static List<OutboxEvent> findOutboxEvents(Session session, int maxResults,
			String queryString, Map<String, Object> params) {
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			query.setParameter( entry.getKey(), entry.getValue() );
		}
		query.setMaxResults( maxResults );
		query.setLockMode( LockModeType.NONE );
		return query.list();
	}

	private final String queryString;
	private final Map<String, Object> params;

	public DefaultOutboxEventFinder(Optional<OutboxEventPredicate> predicate) {
		this.queryString = createQueryString( predicate );
		this.params = predicate.map( OutboxEventPredicate::params ).orElse( Collections.emptyMap() );
	}

	@Override
	public List<OutboxEvent> findOutboxEvents(Session session, int maxResults) {
		return DefaultOutboxEventFinder.findOutboxEvents( session, maxResults, queryString, params );
	}

}
