/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.impl;

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

	private List<OutboxEvent> lockedEvents;
	private List<OutboxEvent> lockedFailedEvents;

	public OutboxEventUpdater(FailureHandler failureHandler, OutboxEventProcessingPlan processingPlan,
			SessionImplementor session, String processorName) {
		this.failureHandler = failureHandler;
		this.processingPlan = processingPlan;
		this.session = session;
		this.processorName = processorName;
		this.eventsIds = processingPlan.getEvents().stream().map( event -> event.getId() ).collect(
				Collectors.toSet() );
		this.failedEventIds = processingPlan.getFailedEvents().stream().map( event -> event.getId() ).collect(
				Collectors.toSet() );
	}

	public void process() {
		lockedEvents = OutboxEventLoader.loadLocking( session, eventsIds, processorName );
		lockedFailedEvents = new ArrayList<>( lockedEvents.size() );

		for ( OutboxEvent event : lockedEvents ) {
			Long id = event.getId();
			eventsIds.remove( id );
			if ( failedEventIds.contains( id ) ) {
				lockedFailedEvents.add( event );
			}
		}

		updateOrDeleteEvents();
	}

	public boolean thereAreStillEventsToProcess() {
		return !eventsIds.isEmpty();
	}

	private void updateOrDeleteEvents() {
		List<OutboxEvent> eventToDelete = new ArrayList<>();
		for ( OutboxEvent event : lockedEvents ) {
			eventToDelete.add( event );
		}

		for ( OutboxEvent failedEvent : lockedFailedEvents ) {
			int attempts = failedEvent.getRetries() + 1;
			if ( attempts >= MAX_RETRIES ) {
				EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
				SearchException exception = log.maxRetryExhausted( MAX_RETRIES );
				builder.throwable( exception );
				builder.failingOperation( "Processing an outbox event." );
				builder.entityReference( processingPlan.entityReference(
						failedEvent.getEntityName(), failedEvent.getEntityId(), exception ) );
				failureHandler.handle( builder.build() );
			}
			else {
				// This is slow, but we don't expect failures often, so that's fine.
				eventToDelete.remove( failedEvent );

				failedEvent.setRetries( attempts );

				log.automaticIndexingRetry( failedEvent.getId(),
						failedEvent.getEntityName(), failedEvent.getEntityId(), attempts
				);
			}
		}

		for ( OutboxEvent event : eventToDelete ) {
			session.delete( event );
		}

		session.flush();
		session.clear();
	}
}
