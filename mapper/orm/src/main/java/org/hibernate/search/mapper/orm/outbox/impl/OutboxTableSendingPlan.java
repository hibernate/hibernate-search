/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.Session;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public class OutboxTableSendingPlan implements AutomaticIndexingQueueEventSendingPlan {

	private final Session session;
	private final List<OutboxEvent> events = new ArrayList<>();

	public OutboxTableSendingPlan(Session session) {
		this.session = session;
	}

	@Override
	public void add(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		events.add( new OutboxEvent( OutboxEvent.Type.ADD, entityName, serializedId, routes, identifier ) );
	}

	@Override
	public void addOrUpdate(String entityName, Object identifier, String serializedId,
			DocumentRoutesDescriptor routes) {
		events.add( new OutboxEvent( OutboxEvent.Type.ADD_OR_UPDATE, entityName, serializedId, routes, identifier ) );
	}

	@Override
	public void delete(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		events.add( new OutboxEvent( OutboxEvent.Type.DELETE, entityName, serializedId, routes, identifier ) );
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
		return CompletableFuture.completedFuture( builder.build() );
	}
}
