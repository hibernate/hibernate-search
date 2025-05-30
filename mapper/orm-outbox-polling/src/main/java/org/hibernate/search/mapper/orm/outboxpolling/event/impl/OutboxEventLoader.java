/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import static org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalMappingProducer.ENTITY_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

final class OutboxEventLoader implements ToStringTreeAppendable {

	private static final String LOAD_QUERY_STRING = "select e from " + ENTITY_NAME + " e where e.id in (:ids)";

	private final LockMode lockMode;

	OutboxEventLoader(Dialect dialect) {
		// HSEARCH-4289: some databases encounter deadlocks when multiple processors query or delete events
		// in concurrent transactions.
		// The deadlocks are mostly caused by lock escalation,
		// e.g. MS SQL deciding it does not have enough resources to perform row-level locks
		// and thus locking whole pages instead. This means that even though processors deal
		// with strictly distinct subsets of the outbox events (thanks to sharding),
		// they will actually end up locking more than their subset,
		// and then conflicts *can* occur.
		// Disabling locks is not an option: we cannot disable locking during deletes.
		// Thus, our last option is to actually enforce locks ahead of time (UPGRADE),
		// and to avoid conflicts by simply never working on events that are already locked (SKIP_LOCKED):
		// that's what LockMode.UPGRADE_SKIPLOCKED is for.
		// That's possible because event processing is not sensitive to processing order,
		// so we can afford to just skip events that are already locked,
		// and process them later when they are no longer locked.
		if ( dialect.supportsSkipLocked() ) {
			lockMode = LockMode.UPGRADE_SKIPLOCKED;
		}
		// If LockMode.UPGRADE_SKIPLOCKED is not supported, we just do basic locking and hope for the best
		// (in particular we hope for transaction deadlocks to be detected by the database and result in a failure,
		// so that we can try again later).
		// HSEARCH-4634: CockroachDB doesn't support LockMode.UPGRADE_SKIPLOCKED
		// (https://github.com/cockroachdb/cockroach/issues/40476?version=v22.1),
		// but fortunately it doesn't suffer from lock escalation,
		// so as long as we target a distinct set of events in each processor (we do),
		// locking shouldn't trigger any deadlocks.
		else {
			lockMode = LockMode.PESSIMISTIC_WRITE;
		}
	}

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.startObject( "lockOptions" )
				.attribute( "lockMode", lockMode )
				.endObject();
	}

	List<OutboxEvent> loadLocking(Session session, Set<UUID> ids, String processorName) {
		try {
			Query<OutboxEvent> query = session.createQuery( LOAD_QUERY_STRING, OutboxEvent.class );
			query.setParameter( "ids", ids );
			query.setHibernateLockMode( lockMode );
			return query.getResultList();
		}
		catch (PessimisticLockException | OptimisticLockException lockException) {
			// Note OptimisticLockException is sometimes (always?) thrown to indicate a *pessimistic* lock failure.
			// It can happen with some databases (Mariadb before 10.6, perhaps others) that do not support
			// skipping locked rows (see LockOptions.UPGRADE_SKIPLOCKED).
			// If that happens, we will just log something and try again later.
			// See also https://jira.mariadb.org/browse/MDEV-13115
			OutboxPollingEventsLog.INSTANCE.outboxEventProcessorUnableToLock( processorName, lockException );
			return Collections.emptyList();
		}
	}
}
