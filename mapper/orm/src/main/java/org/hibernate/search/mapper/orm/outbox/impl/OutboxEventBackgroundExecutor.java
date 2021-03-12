/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import static org.hibernate.search.mapper.orm.common.impl.TransactionUtils.withinTransaction;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public class OutboxEventBackgroundExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int MAX_RETRIALS_LOADED_AT_EACH_TIME = 5000;
	private static final int MAX_RETRIES = 3;

	private final AutomaticIndexingMappingContext mapping;
	private final ScheduledExecutorService executor;
	private final int pollingInterval;
	private final int batchSize;

	public OutboxEventBackgroundExecutor(AutomaticIndexingMappingContext mapping, ScheduledExecutorService executor,
			int pollingInterval, int batchSize) {
		this.mapping = mapping;
		this.executor = executor;
		this.pollingInterval = pollingInterval;
		this.batchSize = batchSize;
	}

	public void start() {
		executor.scheduleAtFixedRate( this::run, 0, pollingInterval, TimeUnit.MILLISECONDS );
	}

	public CompletableFuture<?> stop() {
		executor.shutdown();
		try {
			executor.awaitTermination( 1, TimeUnit.HOURS );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
		return CompletableFuture.completedFuture( null );
	}

	private void run() {
		if ( mapping.sessionFactory().isClosed() ) {
			log.sessionFactoryIsClosedOnOutboxProcessing();
			return;
		}
		try ( Session session = mapping.sessionFactory().openSession() ) {
			try {
				processOutboxRetrialsEntities( session );
			}
			catch (Throwable throwable) {
				log.failureOnProcessingOutboxRetrials();
			}
			try {
				processOutboxEntities( session );
			}
			catch (Throwable throwable) {
				log.failureOnProcessingOutbox();
			}
		}
	}

	private void processOutboxEntities(Session session) {
		List<OutboxEvent> events = findOutboxEntities( session );

		while ( !events.isEmpty() ) {
			OutboxEventProcessing<OutboxEvent> eventProcessing = new OutboxEventProcessing<>(
					mapping, session, events );
			List<Integer> ids = eventProcessing.processEvents();

			try {
				createNewOutboxRetrials( session, eventProcessing.getFailedEvents() );
			}
			catch (Throwable throwable) {
				log.failureOnSaveOutboxRetrials();
			}

			try {
				deleteOutboxEntities( session, ids );
			}
			catch (Throwable throwable) {
				log.failureOnDeleteOutbox();
			}

			events = findOutboxEntities( session );
		}
	}

	private void processOutboxRetrialsEntities(Session session) {
		List<OutboxEventRetry> allEvents = findOutboxRetrialsEntities( session );

		int from = 0;
		int to = Math.min( from + batchSize, allEvents.size() );

		while ( from < allEvents.size() ) {
			List<OutboxEventRetry> events = allEvents.subList( from, to );

			OutboxEventProcessing<OutboxEventRetry> eventProcessing = new OutboxEventProcessing<>(
					mapping, session, events );
			eventProcessing.processEvents();
			Set<OutboxEventRetry> failedEvents = eventProcessing.getFailedEventsSet();

			try {
				for ( OutboxEventRetry event : events ) {
					updateEventState( session, failedEvents, event );
				}
			}
			catch (Throwable throwable) {
				log.failureOnUpdateOutboxRetrials();
			}

			from += batchSize;
			to = Math.min( from + batchSize, allEvents.size() );
		}
	}

	private void updateEventState(Session session, Set<OutboxEventRetry> failedEvents, OutboxEventRetry event) {
		if ( !failedEvents.contains( event ) ) {
			// processed:
			session.delete( event );
			return;
		}

		if ( event.getRetries() > MAX_RETRIES ) {
			log.maxOutboxRetriesExhausted( MAX_RETRIES, event );
			session.delete( event );
			return;
		}

		event.setRetries( event.getRetries() + 1 );
		session.update( event );
	}

	private List<OutboxEvent> findOutboxEntities(Session session) {
		Query<OutboxEvent> query = session.createQuery( "select e from OutboxEvent e order by id", OutboxEvent.class );
		query.setMaxResults( batchSize );
		return query.list();
	}

	private static List<OutboxEventRetry> findOutboxRetrialsEntities(Session session) {
		Query<OutboxEventRetry> query = session.createQuery(
				"select e from OutboxEventRetry e order by id", OutboxEventRetry.class );
		query.setMaxResults( MAX_RETRIALS_LOADED_AT_EACH_TIME );
		return query.list();
	}

	private static int deleteOutboxEntities(Session session, List<Integer> ids) {
		return withinTransaction( session, () -> {
			Query query = session.createQuery( "delete from OutboxEvent e where e.id in :ids" );
			query.setParameter( "ids", ids );
			int update = query.executeUpdate();

			session.flush();
			session.clear();
			return update;
		} );
	}

	private static int createNewOutboxRetrials(Session session,
			Map<OutboxEventReference, List<OutboxEvent>> failedEvents) {
		return withinTransaction( session, () -> {
			for ( Map.Entry<OutboxEventReference, List<OutboxEvent>> entry : failedEvents.entrySet() ) {
				OutboxEventRetry eventRetry = new OutboxEventRetry(
						entry.getKey(), mergeDocumentRoutes( entry.getValue() ) );
				session.persist( eventRetry );
			}

			return 0;
		} );
	}

	public static byte[] mergeDocumentRoutes(List<OutboxEvent> events) {
		if ( events.isEmpty() ) {
			return null;
		}

		if ( events.size() == 1 ) {
			return events.get( 0 ).getDocumentRoutes();
		}

		DocumentRouteDescriptor currentRoute = null;
		Collection<DocumentRouteDescriptor> previousRoutes = new HashSet<>();

		for ( OutboxEvent event : events ) {
			DocumentRoutesDescriptor routes = SerializationUtils.deserialize(
					DocumentRoutesDescriptor.class, event.getDocumentRoutes() );

			// always override the current route
			// TODO HSEARCH-4186 See if the algorithm makes sense
			currentRoute = routes.currentRoute();
			previousRoutes.add( routes.currentRoute() );
			previousRoutes.addAll( routes.previousRoutes() );
		}

		return SerializationUtils.serialize( DocumentRoutesDescriptor.of( currentRoute, previousRoutes ) );
	}
}

