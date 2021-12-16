/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingSearchMappingImpl implements OutboxPollingSearchMapping {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String COUNT_EVENTS_WITH_STATUS = "select count(e) from OutboxEvent e where e.status = :status";
	private static final String UPDATE_EVENTS_WITH_STATUS = "update OutboxEvent e set e.status = :newStatus where e.status = :status";
	private static final String DELETE_EVENTS_WITH_STATUS = "delete OutboxEvent e where e.status = :status";

	private final TransactionHelper transactionHelper;
	private final SessionFactoryImplementor sessionFactory;
	private final Set<String> tenantIds;

	public OutboxPollingSearchMappingImpl(CoordinationStrategyStartContext context, Set<String> tenantIds) {
		this.sessionFactory = context.mapping().sessionFactory();
		this.transactionHelper = new TransactionHelper( sessionFactory );
		this.tenantIds = tenantIds;
	}

	@Override
	public long countAbortedEvents() {
		checkNoTenant();

		AtomicLong result = new AtomicLong();
		try ( Session session = sessionFactory.openSession() ) {
			transactionHelper.inTransaction( (SharedSessionContractImplementor) session, null, s -> {
				Query<Long> query = session.createQuery( COUNT_EVENTS_WITH_STATUS, Long.class );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				result.set( query.getSingleResult() );
			} );
		}
		return result.get();
	}

	@Override
	public long countAbortedEvents(String tenantId) {
		checkTenant( tenantId );

		AtomicLong result = new AtomicLong();
		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			transactionHelper.inTransaction( (SharedSessionContractImplementor) session, null, s -> {
				Query<Long> query = session.createQuery( COUNT_EVENTS_WITH_STATUS, Long.class );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				result.set( query.getSingleResult() );
			} );
		}
		return result.get();
	}

	@Override
	public int reprocessAbortedEvents() {
		checkNoTenant();

		AtomicInteger result = new AtomicInteger();
		try ( Session session = sessionFactory.openSession() ) {
			transactionHelper.inTransaction( (SharedSessionContractImplementor) session, null, s -> {
				Query<?> query = session.createQuery( UPDATE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				query.setParameter( "newStatus", OutboxEvent.Status.PENDING );
				result.set( query.executeUpdate() );
			} );
		}
		return result.get();
	}

	@Override
	public int reprocessAbortedEvents(String tenantId) {
		checkTenant( tenantId );

		AtomicInteger result = new AtomicInteger();
		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			transactionHelper.inTransaction( (SharedSessionContractImplementor) session, null, s -> {
				Query<?> query = session.createQuery( UPDATE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				query.setParameter( "newStatus", OutboxEvent.Status.PENDING );
				result.set( query.executeUpdate() );
			} );
		}
		return result.get();
	}

	@Override
	public int clearAllAbortedEvents() {
		checkNoTenant();

		AtomicInteger result = new AtomicInteger();
		try ( Session session = sessionFactory.openSession() ) {
			transactionHelper.inTransaction( (SharedSessionContractImplementor) session, null, s -> {
				Query<?> query = session.createQuery( DELETE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				result.set( query.executeUpdate() );
			} );
		}
		return result.get();
	}

	@Override
	public int clearAllAbortedEvents(String tenantId) {
		checkTenant( tenantId );

		AtomicInteger result = new AtomicInteger();
		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			transactionHelper.inTransaction( (SharedSessionContractImplementor) session, null, s -> {
				Query<?> query = session.createQuery( DELETE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				result.set( query.executeUpdate() );
			} );
		}
		return result.get();
	}

	private void checkNoTenant() {
		if ( !tenantIds.isEmpty() ) {
			throw log.noTenantIdSpecified( tenantIds );
		}
	}

	private void checkTenant(String tenantId) {
		if ( tenantIds.isEmpty() ) {
			throw log.multiTenancyNotEnabled( tenantId );
		}
		if ( !tenantIds.contains( tenantId ) ) {
			throw log.wrongTenantId( tenantId, tenantIds );
		}
	}
}
