/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.OptimisticLockException;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class OutboxEventLoader {

	private static final String LOAD_QUERY = "select e from OutboxEvent e where e.id in (:ids)";

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private OutboxEventLoader() {
	}

	static List<OutboxEvent> loadLocking(Session session, Set<Long> ids, String processorName) {
		try {
			return tryLoadLocking( session, ids );
		}
		catch (OptimisticLockException lockException) {
			// Don't be fooled by the exception type, this is actually a *pessimistic* lock failure.
			// It can happen with some databases (Mariadb before 10.6, perhaps others) that do not support
			// skipping locked rows (see LockOptions.SKIP).
			// If that happens, we will just log something and try again later.
			// See also https://jira.mariadb.org/browse/MDEV-13115
			log.outboxEventProcessorUnableToLock( processorName, lockException );
			return Collections.emptyList();
		}
	}

	private static List<OutboxEvent> tryLoadLocking(Session session, Set<Long> ids) {
		Query<OutboxEvent> query = session.createQuery( LOAD_QUERY, OutboxEvent.class );
		query.setParameter( "ids", ids );

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

		return query.getResultList();
	}
}
