/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public class OutboxEventProcessing<Event extends OutboxEventBase> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AutomaticIndexingQueueEventProcessingPlan processingPlan;
	private final List<Event> events;
	private final Map<OutboxEventReference, List<Event>> failedEvents = new HashMap<>();
	private final List<Integer> eventsIds;

	public OutboxEventProcessing(AutomaticIndexingMappingContext mapping, Session session, List<Event> events) {
		this.processingPlan = mapping.createIndexingQueueEventProcessingPlan( session );
		this.events = events;
		this.eventsIds = new ArrayList<>( events.size() );
	}

	public List<Integer> processEvents() {
		try {
			addEventsToThePlan();
			reportBackendResult( Futures.unwrappedExceptionGet( processingPlan.executeAndReport() ) );
		}
		catch (Throwable throwable) {
			reportMapperFailure( throwable );
		}

		return eventsIds;
	}

	public Map<OutboxEventReference, List<Event>> getFailedEvents() {
		return failedEvents;
	}

	public Set<Event> getFailedEventsSet() {
		return failedEvents.values().stream().flatMap( events -> events.stream() ).collect( Collectors.toSet() );
	}

	private void addEventsToThePlan() {
		for ( Event event : events ) {
			DocumentRoutesDescriptor routes = getRoutes( event );

			switch ( event.getType() ) {
				case ADD:
					processingPlan.add( event.getEntityName(), event.getEntityId(), routes );
					break;
				case ADD_OR_UPDATE:
					processingPlan.addOrUpdate( event.getEntityName(), event.getEntityId(), routes );
					break;
				case DELETE:
					processingPlan.delete( event.getEntityName(), event.getEntityId(), routes );
					break;
			}
			eventsIds.add( event.getId() );
		}
	}

	private DocumentRoutesDescriptor getRoutes(Event event) {
		return SerializationUtils.deserialize( DocumentRoutesDescriptor.class, event.getDocumentRoutes() );
	}

	private void reportMapperFailure(Throwable throwable) {
		try {
			// Something failed, but we don't know what.
			// Assume all events failed.
			reportAllEventsFailure( throwable, getEventsByReferences() );
		}
		catch (Throwable t) {
			throwable.addSuppressed( t );
		}
	}

	private void reportBackendResult(MultiEntityOperationExecutionReport<EntityReference> report) {
		if ( !report.throwable().isPresent() ) {
			return;
		}

		Map<OutboxEventReference, List<Event>> eventsMap = getEventsByReferences();
		for ( EntityReference entityReference : report.failingEntityReferences() ) {
			OutboxEventReference outboxEventReference = new OutboxEventReference(
					entityReference.name(),
					processingPlan.toSerializedId( entityReference.name(), entityReference.id() )
			);

			failedEvents.put( outboxEventReference, eventsMap.get( outboxEventReference ) );
		}

		log.failureToReindexOutboxEntities( report.failingEntityReferences(), report.throwable().get() );
	}

	private void reportAllEventsFailure(Throwable throwable, Map<OutboxEventReference, List<Event>> eventsMap) {
		failedEvents.putAll( eventsMap );
		for ( List<Event> events : eventsMap.values() ) {
			for ( Event event : events ) {
				log.failureOnProcessingOutboxEvent( event, throwable );
			}
		}
	}

	private Map<OutboxEventReference, List<Event>> getEventsByReferences() {
		Map<OutboxEventReference, List<Event>> eventsMap = new HashMap<>();
		for ( Event event : events ) {
			eventsMap.computeIfAbsent( event.getReference(), key -> new ArrayList<>() );
			eventsMap.get( event.getReference() ).add( event );
		}
		return eventsMap;
	}
}
