/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.impl.EventPayloadSerializationUtils;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.impl.Futures;

final class OutboxEventProcessingPlan {

	private final AutomaticIndexingQueueEventProcessingPlan processingPlan;
	private final FailureHandler failureHandler;
	private final EntityReferenceFactory entityReferenceFactory;
	private final List<OutboxEvent> failedEvents = new ArrayList<>();

	private List<OutboxEvent> events = new ArrayList<>();

	public OutboxEventProcessingPlan(AutomaticIndexingMappingContext mapping, Session session) {
		this.processingPlan = mapping.createIndexingQueueEventProcessingPlan( session );
		this.failureHandler = mapping.failureHandler();
		this.entityReferenceFactory = mapping.entityReferenceFactory();
	}

	void processEvents(List<OutboxEvent> events) {
		this.events = events;
		try {
			addEventsToThePlan();
			reportBackendResult(
					Futures.unwrappedExceptionGet( processingPlan.executeAndReport( OperationSubmitter.blocking() ) ) );
		}
		catch (Throwable throwable) {
			if ( throwable instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			reportMapperFailure( throwable );
		}
	}

	List<OutboxEvent> getEvents() {
		return events;
	}

	List<OutboxEvent> getFailedEvents() {
		return failedEvents;
	}

	EntityReference entityReference(String entityName, String entityId, Throwable throwable) {
		try {
			Object identifier = processingPlan.toIdentifier( entityName, entityId );
			return EntityReferenceFactory.safeCreateEntityReference(
					entityReferenceFactory, entityName, identifier, throwable::addSuppressed );
		}
		catch (RuntimeException e) {
			// We failed to extract a reference.
			// Let's just give up and suppress the exception.
			throwable.addSuppressed( e );
			return null;
		}
	}

	private void addEventsToThePlan() {
		for ( OutboxEvent event : events ) {
			PojoIndexingQueueEventPayload payload = EventPayloadSerializationUtils.deserialize( event.getPayload() );
			processingPlan.append( event.getEntityName(), event.getEntityId(), payload );
		}
	}

	private void reportMapperFailure(Throwable throwable) {
		try {
			// Something failed, but we don't know what.
			// Assume all events failed.
			reportAllEventsFailure( throwable );
		}
		catch (Throwable t) {
			throwable.addSuppressed( t );
		}
	}

	private void reportBackendResult(MultiEntityOperationExecutionReport report) {
		Optional<Throwable> throwable = report.throwable();
		if ( !throwable.isPresent() ) {
			return;
		}

		Map<OutboxEventReference, List<OutboxEvent>> eventsMap = getEventsByReferences();
		EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
		builder.throwable( throwable.get() );
		builder.failingOperation( "Processing an outbox event." );

		for ( EntityReference entityReference : report.failingEntityReferences() ) {
			OutboxEventReference outboxEventReference = new OutboxEventReference(
					entityReference.name(),
					extractReferenceOrSuppress( entityReference, throwable.get() )
			);

			builder.failingEntityReference( entityReference );
			failedEvents.addAll( eventsMap.get( outboxEventReference ) );
		}
		failureHandler.handle( builder.build() );
	}

	private String extractReferenceOrSuppress(EntityReference entityReference, Throwable throwable) {
		try {
			return processingPlan.toSerializedId( entityReference.name(), entityReference.id() );
		}
		catch (RuntimeException e) {
			throwable.addSuppressed( e );
			return null;
		}
	}

	private void reportAllEventsFailure(Throwable throwable) {
		failedEvents.addAll( events );
		EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
		builder.throwable( throwable );
		builder.failingOperation( "Processing an outbox event." );

		for ( OutboxEvent event : events ) {
			builder.failingEntityReference( entityReference( event.getEntityName(), event.getEntityId(), throwable ) );
		}
		failureHandler.handle( builder.build() );
	}

	private Map<OutboxEventReference, List<OutboxEvent>> getEventsByReferences() {
		Map<OutboxEventReference, List<OutboxEvent>> eventsMap = new HashMap<>();
		for ( OutboxEvent event : events ) {
			eventsMap.computeIfAbsent( event.getReference(), key -> new ArrayList<>() );
			eventsMap.get( event.getReference() ).add( event );
		}
		return eventsMap;
	}
}
