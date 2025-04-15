/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.outboxpolling.avro.impl.EventPayloadSerializationUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.data.impl.RangeCompatibleHashFunction;

public final class OutboxPollingOutboxEventSendingPlan implements AutomaticIndexingQueueEventSendingPlan {

	// Note the hash function / table implementations MUST NOT CHANGE,
	// otherwise existing indexes will no longer work correctly.
	private static final RangeCompatibleHashFunction HASH_FUNCTION = ShardAssignment.HASH_FUNCTION;

	private final EntityReferenceFactory entityReferenceFactory;
	private final Session session;
	private final List<OutboxEvent> events = new ArrayList<>();

	public OutboxPollingOutboxEventSendingPlan(EntityReferenceFactory entityReferenceFactory,
			Session session) {
		this.entityReferenceFactory = entityReferenceFactory;
		this.session = session;
	}

	@Override
	public void append(String entityName, Object identifier, String serializedId,
			PojoIndexingQueueEventPayload payload) {
		events.add( new OutboxEvent( entityName, serializedId,
				HASH_FUNCTION.hash( serializedId ),
				EventPayloadSerializationUtils.serialize( payload ),
				identifier
		) );
	}

	@Override
	public void discard() {
		events.clear();
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport> sendAndReport(OperationSubmitter operationSubmitter) {
		if ( !OperationSubmitter.blocking().equals( operationSubmitter ) ) {
			throw OutboxPollingEventsLog.INSTANCE.nonblockingOperationSubmitterNotSupported();
		}

		if ( session.isOpen() ) {
			return sendAndReportOnSession( session, entityReferenceFactory );
		}

		// See https://hibernate.atlassian.net/browse/HSEARCH-4198
		// When JTA is enabled with Spring, the shouldReleaseBeforeCompletion strategy is used by default.
		// Solution inspired by org.hibernate.envers.internal.synchronization.AuditProcess#doBeforeTransactionCompletion.
		try ( Session temporarySession = session.sessionWithOptions()
				.connection()
				.autoClose( false )
				.connectionHandling( ConnectionAcquisitionMode.AS_NEEDED, ConnectionReleaseMode.AFTER_TRANSACTION )
				.openSession() ) {
			return sendAndReportOnSession( temporarySession, entityReferenceFactory );
		}
	}

	private CompletableFuture<MultiEntityOperationExecutionReport> sendAndReportOnSession(
			Session currentSession, EntityReferenceFactory entityReferenceFactory) {
		try {
			MultiEntityOperationExecutionReport.Builder builder = MultiEntityOperationExecutionReport.builder();
			for ( OutboxEvent event : events ) {
				try {
					currentSession.persist( event );
				}
				catch (RuntimeException e) {
					builder.throwable( e );
					builder.failingEntityReference(
							entityReferenceFactory, event.getEntityName(), event.getOriginalEntityId() );
				}
			}
			currentSession.flush();
			OutboxPollingEventsLog.INSTANCE.eventPlanNumberOfPersistedEvents( events.size(), events );
			return CompletableFuture.completedFuture( builder.build() );
		}
		finally {
			events.clear();
		}
	}
}
