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
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.outboxpolling.avro.impl.EventPayloadSerializationUtils;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.data.impl.RangeCompatibleHashFunction;

public abstract class OutboxPollingOutboxEventSendingPlan implements AutomaticIndexingQueueEventSendingPlan {

	// Note the hash function / table implementations MUST NOT CHANGE,
	// otherwise existing indexes will no longer work correctly.
	private static final RangeCompatibleHashFunction HASH_FUNCTION = ShardAssignment.HASH_FUNCTION;

	public static OutboxPollingOutboxEventSendingPlan create(EntityReferenceFactory entityReferenceFactory,
			SharedSessionContract session) {
		if ( session instanceof StatelessSession statelessSession ) {
			return new StatelessSessionOutboxPollingOutboxEventSendingPlan( entityReferenceFactory, statelessSession );
		}
		return new SessionOutboxPollingOutboxEventSendingPlan( entityReferenceFactory, (Session) session );
	}

	protected final EntityReferenceFactory entityReferenceFactory;
	protected final List<OutboxEvent> events = new ArrayList<>();

	private OutboxPollingOutboxEventSendingPlan(EntityReferenceFactory entityReferenceFactory) {
		this.entityReferenceFactory = entityReferenceFactory;
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

	private static final class SessionOutboxPollingOutboxEventSendingPlan extends OutboxPollingOutboxEventSendingPlan {
		private final Session session;

		private SessionOutboxPollingOutboxEventSendingPlan(EntityReferenceFactory entityReferenceFactory, Session session) {
			super( entityReferenceFactory );
			this.session = session;
		}

		@Override
		public CompletableFuture<MultiEntityOperationExecutionReport> sendAndReport(
				OperationSubmitter operationSubmitter) {
			if ( !OperationSubmitter.blocking().equals( operationSubmitter ) ) {
				throw OutboxPollingEventsLog.INSTANCE.nonblockingOperationSubmitterNotSupported();
			}

			if ( session.isOpen() ) {
				return doSendAndReport( session );
			}

			// See https://hibernate.atlassian.net/browse/HSEARCH-4198
			// When JTA is enabled with Spring, the shouldReleaseBeforeCompletion strategy is used by default.
			// Solution inspired by org.hibernate.envers.internal.synchronization.AuditProcess#doBeforeTransactionCompletion.
			try ( Session temporarySession = session.sessionWithOptions()
					.connection()
					.autoClose( false )
					.connectionHandling( ConnectionAcquisitionMode.AS_NEEDED, ConnectionReleaseMode.AFTER_TRANSACTION )
					.openSession() ) {
				return doSendAndReport( temporarySession );
			}
		}

		private CompletableFuture<MultiEntityOperationExecutionReport> doSendAndReport(Session currentSession) {
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

	private static final class StatelessSessionOutboxPollingOutboxEventSendingPlan extends OutboxPollingOutboxEventSendingPlan {
		private final StatelessSession session;

		private StatelessSessionOutboxPollingOutboxEventSendingPlan(EntityReferenceFactory entityReferenceFactory,
				StatelessSession session) {
			super( entityReferenceFactory );
			this.session = session;
		}

		@Override
		public CompletableFuture<MultiEntityOperationExecutionReport> sendAndReport(
				OperationSubmitter operationSubmitter) {
			if ( !OperationSubmitter.blocking().equals( operationSubmitter ) ) {
				throw OutboxPollingEventsLog.INSTANCE.nonblockingOperationSubmitterNotSupported();
			}

			try {
				MultiEntityOperationExecutionReport.Builder builder = MultiEntityOperationExecutionReport.builder();
				for ( OutboxEvent event : events ) {
					try {
						session.insert( event );
					}
					catch (RuntimeException e) {
						builder.throwable( e );
						builder.failingEntityReference(
								entityReferenceFactory, event.getEntityName(), event.getOriginalEntityId() );
					}
				}
				OutboxPollingEventsLog.INSTANCE.eventPlanNumberOfPersistedEvents( events.size(), events );
				return CompletableFuture.completedFuture( builder.build() );
			}
			finally {
				events.clear();
			}
		}
	}
}
