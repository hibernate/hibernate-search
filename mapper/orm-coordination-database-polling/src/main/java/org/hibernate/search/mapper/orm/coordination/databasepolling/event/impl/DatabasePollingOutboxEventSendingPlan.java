/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.Session;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.coordination.databasepolling.avro.impl.EventPayloadSerializationUtils;
import org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.data.impl.Murmur3HashFunction;
import org.hibernate.search.util.common.data.impl.RangeCompatibleHashFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DatabasePollingOutboxEventSendingPlan implements AutomaticIndexingQueueEventSendingPlan {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Note the hash function / table implementations MUST NOT CHANGE,
	// otherwise existing indexes will no longer work correctly.
	public static final RangeCompatibleHashFunction HASH_FUNCTION = Murmur3HashFunction.INSTANCE;

	private final Session session;
	private final List<OutboxEvent> events = new ArrayList<>();

	public DatabasePollingOutboxEventSendingPlan(Session session) {
		this.session = session;
	}

	@Override
	public void append(String entityName, Object identifier, String serializedId,
			PojoIndexingQueueEventPayload payload) {
		events.add( new OutboxEvent( entityName, serializedId,
				DatabasePollingOutboxEventSendingPlan.HASH_FUNCTION.hash( serializedId ),
				EventPayloadSerializationUtils.serialize( payload ),
				identifier
		) );
	}

	@Override
	public void discard() {
		events.clear();
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> sendAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		if ( session.isOpen() ) {
			return sendAndReportOnSession( session, entityReferenceFactory );
		}

		// See https://hibernate.atlassian.net/browse/HSEARCH-4198
		// When JTA is enabled with Spring, the shouldReleaseBeforeCompletion strategy is used by default.
		// Solution inspired by org.hibernate.envers.internal.synchronization.AuditProcess#doBeforeTransactionCompletion.
		try ( Session temporarySession = session.sessionWithOptions()
				.connection()
				.autoClose( false )
				.connectionHandlingMode(
						PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION )
				.openSession() ) {
			return sendAndReportOnSession( temporarySession, entityReferenceFactory );
		}
	}

	private <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> sendAndReportOnSession(
			Session currentSession, EntityReferenceFactory<R> entityReferenceFactory) {
		try {
			MultiEntityOperationExecutionReport.Builder<R> builder = MultiEntityOperationExecutionReport.builder();
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
			log.tracef( "Persisted %d outbox events: '%s'", events.size(), events );
			return CompletableFuture.completedFuture( builder.build() );
		}
		finally {
			events.clear();
		}
	}
}
