/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.ENTITY_ID_PROPERTY_NAME;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.ENTITY_NAME_PROPERTY_NAME;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.OUTBOX_ENTITY_NAME;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.ROUTE_PROPERTY_NAME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public class OutboxTableSendingPlan implements AutomaticIndexingQueueEventSendingPlan {

	private final Session session;
	private final Set<OutboxEvent> events = new HashSet<>();

	public OutboxTableSendingPlan(Session session) {
		this.session = session;
	}

	@Override
	public void add(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		events.add( new OutboxEvent( entityName, serializedId, routes ) );
	}

	@Override
	public void addOrUpdate(String entityName, Object identifier, String serializedId,
			DocumentRoutesDescriptor routes) {
		events.add( new OutboxEvent( entityName, serializedId, routes ) );
	}

	@Override
	public void delete(String entityName, Object identifier, String serializedId, DocumentRoutesDescriptor routes) {
		events.add( new OutboxEvent( entityName, serializedId, routes ) );
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
				HashMap<String, Object> entityData = new HashMap<>();
				entityData.put( ENTITY_NAME_PROPERTY_NAME, event.getEntityName() );
				entityData.put( ENTITY_ID_PROPERTY_NAME, event.getSerializedId() );
				entityData.put( ROUTE_PROPERTY_NAME, event.getSerializedRoutes() );

				session.persist( OUTBOX_ENTITY_NAME, entityData );
			}
			catch (RuntimeException e) {
				builder.throwable( e );
				builder.failingEntityReference( entityReferenceFactory, event.getEntityName(), event.getSerializedId() );
			}
		}
		session.flush();
		return CompletableFuture.completedFuture( builder.build() );
	}
}
