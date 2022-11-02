/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.OptimisticLockException;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class OutboxEventLoader {

	private static final String LOAD_QUERY = "select e from OutboxEvent e where e.id in (:ids)";

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static OutboxEventLoader create(Dialect dialect) {
		if ( dialect.supportsSkipLocked() ) {
			return new SkipLockedOutboxEventLoader();
		}
		// If SKIP_LOCKED is not supported, we just do basic locking and hope for the best
		// (in particular we hope for transaction deadlocks to be detected by the database and result in a failure,
		// so that we can try again later).
		// See also the javadoc of LockAllOutboxEventLoader.
		return new LockAllOutboxEventLoader();
	}

	OutboxEventLoader() {
	}

	List<OutboxEvent> loadLocking(Session session, Set<UUID> ids, String processorName) {
		try {
			return tryLoadLocking( session, ids );
		}
		catch (OptimisticLockException lockException) {
			// Don't be fooled by the exception type, this is actually a *pessimistic* lock failure.
			// It can happen with some databases (Mariadb before 10.6, perhaps others) that do not support
			// skipping locked rows (see LockOptions.SKIP_LOCKED).
			// If that happens, we will just log something and try again later.
			// See also https://jira.mariadb.org/browse/MDEV-13115
			log.outboxEventProcessorUnableToLock( processorName, lockException );
			return Collections.emptyList();
		}
	}

	protected abstract List<OutboxEvent> tryLoadLocking(Session session, Set<UUID> ids);

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
	private static class SkipLockedOutboxEventLoader extends OutboxEventLoader {
		@Override
		protected List<OutboxEvent> tryLoadLocking(Session session, Set<UUID> ids) {
			Query<OutboxEvent> query = session.createQuery( LOAD_QUERY, OutboxEvent.class );
			query.setParameter( "ids", ids );

			query.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeOut( LockOptions.SKIP_LOCKED ) );

			return query.getResultList();
		}
	}

	// HSEARCH-4634: CockroachDB doesn't support SKIP_LOCKED (https://github.com/cockroachdb/cockroach/issues/40476?version=v22.1),
	// but fortunately it doesn't suffer from lock escalation,
	// so as long as we target a distinct set of events in each processor (we do),
	// locking shouldn't trigger any deadlocks.
	private static class LockAllOutboxEventLoader extends OutboxEventLoader {
		@Override
		protected List<OutboxEvent> tryLoadLocking(Session session, Set<UUID> ids) {
			Query<OutboxEvent> query = session.createQuery( LOAD_QUERY, OutboxEvent.class );
			query.setParameter( "ids", ids );

			query.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) );

			return query.getResultList();
		}
	}
}
