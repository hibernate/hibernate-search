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
		MultiEntityOperationExecutionReport.Builder<R> builder = MultiEntityOperationExecutionReport.builder();
		for ( OutboxEvent event : events ) {
			try {
				session.persist( event );
			}
			catch (RuntimeException e) {
				builder.throwable( e );
				builder.failingEntityReference(
						entityReferenceFactory, event.getEntityName(), event.getOriginalEntityId() );
			}
		}
		session.flush();
		return CompletableFuture.completedFuture( builder.build() );
	}
}
