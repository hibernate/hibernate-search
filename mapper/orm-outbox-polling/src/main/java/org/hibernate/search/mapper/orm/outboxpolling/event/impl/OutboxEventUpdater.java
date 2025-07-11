/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.util.common.SearchException;

public class OutboxEventUpdater {

	private static final int MAX_RETRIES = 3;

	private final FailureHandler failureHandler;
	private final OutboxEventLoader loader;
	private final OutboxEventProcessingPlan processingPlan;
	private final SessionImplementor session;
	private final String processorName;
	private final int retryAfter;
	private final Set<UUID> eventsIds;
	private final Set<UUID> failedEventIds;

	public OutboxEventUpdater(FailureHandler failureHandler, OutboxEventLoader loader,
			OutboxEventProcessingPlan processingPlan, SessionImplementor session, String processorName, int retryAfter) {
		this.failureHandler = failureHandler;
		this.loader = loader;
		this.processingPlan = processingPlan;
		this.session = session;
		this.processorName = processorName;
		this.retryAfter = retryAfter;
		this.eventsIds = processingPlan.getEvents().stream().map( OutboxEvent::getId )
				.collect( Collectors.toSet() );
		this.failedEventIds = processingPlan.getFailedEvents().stream().map( OutboxEvent::getId )
				.collect( Collectors.toSet() );
	}

	public boolean thereAreStillEventsToProcess() {
		return !eventsIds.isEmpty();
	}

	public Set<UUID> eventsToProcess() {
		return Collections.unmodifiableSet( eventsIds );
	}

	public void process() {
		List<OutboxEvent> lockedEvents = loader.loadLocking( session, eventsIds, processorName );
		List<OutboxEvent> eventToDelete = new ArrayList<>( lockedEvents );

		for ( OutboxEvent event : lockedEvents ) {
			UUID id = event.getId();
			// Make sure we consider the event as processed in "thereAreStillEventsToProcess()"
			eventsIds.remove( id );

			if ( !failedEventIds.contains( id ) ) {
				// The event was processed successfully; we will simply delete it.
				continue;
			}

			// We won't delete this event.
			eventToDelete.remove( event );

			// Failed events have to be processed differently:
			// we try to update their retry count instead of deleting them,
			// so that the process will try to process them again.
			int attempts = event.getRetries() + 1;
			if ( attempts >= MAX_RETRIES ) {
				notifyMaxRetriesReached( event );
				event.setStatus( OutboxEvent.Status.ABORTED );
			}
			else {
				// We will simply increment the retry count of this event,
				// and the event processor will process it once more in the next batch
				event.setRetries( attempts );

				Instant processAfter = ( retryAfter > 0 ) ? Instant.now().plusSeconds( retryAfter ) : Instant.now();
				event.setProcessAfter( processAfter );

				OutboxPollingEventsLog.INSTANCE.backgroundIndexingRetry(
						event.getId(), event.getEntityName(), event.getEntityId(), attempts, processAfter
				);
			}
		}

		for ( OutboxEvent event : eventToDelete ) {
			session.remove( event );
		}
	}

	private void notifyMaxRetriesReached(OutboxEvent failedEvent) {
		EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
		SearchException exception = OutboxPollingEventsLog.INSTANCE.maxRetryExhausted( MAX_RETRIES );
		builder.throwable( exception );
		builder.failingOperation( "Processing an outbox event." );
		builder.failingEntityReference( processingPlan.entityReference(
				failedEvent.getEntityName(), failedEvent.getEntityId(), exception ) );
		failureHandler.handle( builder.build() );
	}
}
