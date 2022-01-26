/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl;

import static org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer.ENTITY_NAME;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.OutboxPollingSearchMapping;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingSearchMappingImpl implements OutboxPollingSearchMapping {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String COUNT_EVENTS_WITH_STATUS = "select count(e) from " + ENTITY_NAME + " e where e.status = :status";
	private static final String UPDATE_EVENTS_WITH_STATUS = "update " + ENTITY_NAME + " e set e.status = :newStatus where e.status = :status";
	private static final String DELETE_EVENTS_WITH_STATUS = "delete " + ENTITY_NAME + " e where e.status = :status";

	private final TransactionHelper transactionHelper;
	private final SessionFactoryImplementor sessionFactory;
	private final TenancyConfiguration tenancyConfiguration;
	private final Set<String> tenantIds;

	public OutboxPollingSearchMappingImpl(CoordinationStrategyStartContext context,
			TenancyConfiguration tenancyConfiguration) {
		this.sessionFactory = context.mapping().sessionFactory();
		this.transactionHelper = new TransactionHelper( sessionFactory, null );
		this.tenancyConfiguration = tenancyConfiguration;
		this.tenantIds = this.tenancyConfiguration.tenantIdsOrFail();
	}

	@Override
	public long countAbortedEvents() {
		checkNoTenant();

		try ( Session session = sessionFactory.openSession() ) {
			return transactionHelper.inTransaction( (SharedSessionContractImplementor) session, () -> {
				Query<Long> query = session.createQuery( COUNT_EVENTS_WITH_STATUS, Long.class );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				return query.getSingleResult();
			} );
		}
	}

	@Override
	public long countAbortedEvents(String tenantId) {
		checkTenant( tenantId );

		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			return transactionHelper.inTransaction( (SharedSessionContractImplementor) session, () -> {
				Query<Long> query = session.createQuery( COUNT_EVENTS_WITH_STATUS, Long.class );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				return query.getSingleResult();
			} );
		}
	}

	@Override
	public int reprocessAbortedEvents() {
		checkNoTenant();

		try ( Session session = sessionFactory.openSession() ) {
			return transactionHelper.inTransaction( (SharedSessionContractImplementor) session, () -> {
				MutationQuery query = session.createMutationQuery( UPDATE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				query.setParameter( "newStatus", OutboxEvent.Status.PENDING );
				return query.executeUpdate();
			} );
		}
	}

	@Override
	public int reprocessAbortedEvents(String tenantId) {
		checkTenant( tenantId );

		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			return transactionHelper.inTransaction( (SharedSessionContractImplementor) session, () -> {
				MutationQuery query = session.createMutationQuery( UPDATE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				query.setParameter( "newStatus", OutboxEvent.Status.PENDING );
				return query.executeUpdate();
			} );
		}
	}

	@Override
	public int clearAllAbortedEvents() {
		checkNoTenant();

		try ( Session session = sessionFactory.openSession() ) {
			return transactionHelper.inTransaction( (SharedSessionContractImplementor) session, () -> {
				MutationQuery query = session.createMutationQuery( DELETE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				return query.executeUpdate();
			} );
		}
	}

	@Override
	public int clearAllAbortedEvents(String tenantId) {
		checkTenant( tenantId );

		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			return transactionHelper.inTransaction( (SharedSessionContractImplementor) session, () -> {
				MutationQuery query = session.createMutationQuery( DELETE_EVENTS_WITH_STATUS );
				query.setParameter( "status", OutboxEvent.Status.ABORTED );
				return query.executeUpdate();
			} );
		}
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
			throw tenancyConfiguration.invalidTenantId( tenantId );
		}
	}
}
