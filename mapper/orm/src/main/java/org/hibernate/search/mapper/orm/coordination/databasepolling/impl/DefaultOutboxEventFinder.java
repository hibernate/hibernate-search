/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
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
				+ " order by e.id";
	}

	public static Query<OutboxEvent> createQuery(Session session, int maxResults,
			String queryString, Map<String, Object> params) {
		Query<OutboxEvent> query = session.createQuery( queryString, OutboxEvent.class );
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			query.setParameter( entry.getKey(), entry.getValue() );
		}
		query.setMaxResults( maxResults );
		// HSEARCH-4289: some databases encounter deadlocks when multiple processors query or delete events
		// in concurrent transactions.
		// The deadlocks are mostly caused by lock escalation,
		// e.g. MS SQL deciding it does not have enough resources to perform row-level locks
		// and thus locking whole pages instead. This means that even though processors deal
		// with strictly distinct subsets of the outbox events (thanks to sharding),
		// they will actually end up locking more than their subset,
		// and then conflicts *can* occur.
		// Disabling locks is not an option: we cannot disable locking during deletes.
		// Thus, our last option is to actually enforce locks ahead of time (LockModeType.PESSIMISTIC_WRITE),
		// and to avoid conflicts by simply never working on events that are already locked (LockOptions.SKIP_LOCKED).
		// That's possible because event processing is not sensitive to processing order,
		// so we can afford to just skip events that are already locked,
		// and process them later when they are no longer locked.
		query.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeOut( LockOptions.SKIP_LOCKED ) );
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

}
