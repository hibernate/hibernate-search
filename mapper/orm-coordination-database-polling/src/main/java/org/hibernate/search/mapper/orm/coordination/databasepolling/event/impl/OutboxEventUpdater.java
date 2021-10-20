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
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl.Log;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxEventUpdater {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final int MAX_RETRIES = 3;

	private final FailureHandler failureHandler;
	private final OutboxEventProcessingPlan processingPlan;
	private final SessionImplementor session;
	private final String processorName;
	private final Set<Long> eventsIds;
	private final Set<Long> failedEventIds;

	public OutboxEventUpdater(FailureHandler failureHandler, OutboxEventProcessingPlan processingPlan,
			SessionImplementor session, String processorName) {
		this.failureHandler = failureHandler;
		this.processingPlan = processingPlan;
		this.session = session;
		this.processorName = processorName;
		this.eventsIds = processingPlan.getEvents().stream().map( OutboxEvent::getId )
				.collect( Collectors.toSet() );
		this.failedEventIds = processingPlan.getFailedEvents().stream().map( OutboxEvent::getId )
				.collect( Collectors.toSet() );
	}

	public boolean thereAreStillEventsToProcess() {
		return !eventsIds.isEmpty();
	}

	public void process() {
		List<OutboxEvent> lockedEvents = OutboxEventLoader.loadLocking( session, eventsIds, processorName );
		List<OutboxEvent> eventToDelete = new ArrayList<>( lockedEvents );

		for ( OutboxEvent event : lockedEvents ) {
			Long id = event.getId();
			// Make sure we consider the event as processed in "thereAreStillEventsToProcess()"
			eventsIds.remove( id );

			if ( !failedEventIds.contains( id ) ) {
				// The event was processed successfully; we will simply delete it.
				continue;
			}

			// Failed events have to be processed differently:
			// we try to update their retry count instead of deleting them,
			// so that the process will try to process them again.
			int attempts = event.getRetries() + 1;
			if ( attempts >= MAX_RETRIES ) {
				notifyMaxRetriesReached( event );
				// We will delete this event, even if it was not processed correctly
				// TODO HSEARCH-4283 Try to persist the event somewhere instead
			}
			else {
				// We won't delete this event.
				eventToDelete.remove( event );

				// We will simply increment the retry count of this event,
				// and the event processor will process it once more in the next batch.
				event.setRetries( attempts );

				log.automaticIndexingRetry( event.getId(),
						event.getEntityName(), event.getEntityId(), attempts
				);
			}
		}

		for ( OutboxEvent event : eventToDelete ) {
			session.delete( event );
		}
	}

	private void notifyMaxRetriesReached(OutboxEvent failedEvent) {
		EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
		SearchException exception = log.maxRetryExhausted( MAX_RETRIES );
		builder.throwable( exception );
		builder.failingOperation( "Processing an outbox event." );
		builder.entityReference( processingPlan.entityReference(
				failedEvent.getEntityName(), failedEvent.getEntityId(), exception ) );
		failureHandler.handle( builder.build() );
	}
}
