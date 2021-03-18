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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public class OutboxEventBackgroundExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final int MAX_RETRIES = 3;

	private enum Status {
		STOPPED,
		STARTED
	}

	private final AutomaticIndexingMappingContext mapping;
	private final int pollingInterval;
	private final int batchSize;
	private final AtomicReference<Status> status = new AtomicReference<>( Status.STOPPED );
	private final SingletonTask processingTask;

	public OutboxEventBackgroundExecutor(AutomaticIndexingMappingContext mapping, ScheduledExecutorService executor,
			int pollingInterval, int batchSize) {
		this.mapping = mapping;
		this.pollingInterval = pollingInterval;
		this.batchSize = batchSize;

		processingTask = new SingletonTask(
				"Delayed commit for " + OutboxTableAutomaticIndexingStrategy.NAME,
				new HibernateOrmOutboxWorker(),
				new HibernateOrmOutboxScheduler( executor ),
				mapping.failureHandler()
		);
	}

	public void start() {
		status.set( Status.STARTED );
		processingTask.ensureScheduled();
	}

	public CompletableFuture<?> completion() {
		status.set( Status.STOPPED );
		return processingTask.completion();
	}

	public void stop() {
		processingTask.stop();
	}

	private class HibernateOrmOutboxWorker implements SingletonTask.Worker {

		@Override
		public CompletableFuture<?> work() {
			if ( mapping.sessionFactory().isClosed() ) {
				log.sessionFactoryIsClosedOnOutboxProcessing();
				return CompletableFuture.completedFuture( null );
			}

			try ( Session session = mapping.sessionFactory().openSession() ) {
				// TODO HSEARCH-4194 Guarantee a minimum delay on processing retries of outbox events
				List<OutboxEventRetry> outboxRetries = findOutboxRetries( session );
				if ( !outboxRetries.isEmpty() ) {
					// Process the event retries
					OutboxEventProcessingPlan<OutboxEventRetry> eventProcessing = new OutboxEventProcessingPlan<>(
							mapping, session, outboxRetries );
					eventProcessing.processEvents();
					Set<OutboxEventRetry> failedEvents = eventProcessing.getFailedEventsSet();
					for ( OutboxEventRetry event : outboxRetries ) {
						deleteOrUpdateOutboxRetries( session, event, !failedEvents.contains( event ) );
					}
				}

				List<OutboxEvent> outboxes = findOutboxes( session );
				if ( outboxes.isEmpty() ) {
					// Nothing to do, try again later (complete() will be called, re-scheduling the polling for later)
					return CompletableFuture.completedFuture( null );
				}

				// There are events to process
				// Make sure we will process the next batch ASAP
				// Since the worker is already working,
				// calling ensureScheduled() will lead to immediate re-execution right after we're done
				processingTask.ensureScheduled();

				// Process the events
				OutboxEventProcessingPlan<OutboxEvent> eventProcessing = new OutboxEventProcessingPlan<>(
						mapping, session, outboxes );
				List<Integer> ids = eventProcessing.processEvents();
				createOutboxRetries( session, eventProcessing.getFailedEvents() );
				deleteOutboxes( session, ids );

				return CompletableFuture.completedFuture( null );
			}
		}

		@Override
		public void complete() {
			if ( status.get() == Status.STARTED ) {
				// Make sure we poll again in a few seconds
				// Since the worker is no longer working,
				// calling ensureScheduled() will lead to delayed re-execution
				processingTask.ensureScheduled();
			}
		}
	}

	private class HibernateOrmOutboxScheduler implements SingletonTask.Scheduler {
		private final ScheduledExecutorService delegate;

		private HibernateOrmOutboxScheduler(ScheduledExecutorService delegate) {
			this.delegate = delegate;
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			return delegate.schedule( runnable, pollingInterval, TimeUnit.MILLISECONDS );
		}
	}

	private List<OutboxEventRetry> findOutboxRetries(Session session) {
		Query<OutboxEventRetry> query = session.createQuery(
				"select e from OutboxEventRetry e order by id", OutboxEventRetry.class );
		query.setMaxResults( batchSize );
		return query.list();
	}

	private void deleteOrUpdateOutboxRetries(Session session, OutboxEventRetry event, boolean processed) {
		if ( processed ) {
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

	private List<OutboxEvent> findOutboxes(Session session) {
		Query<OutboxEvent> query = session.createQuery( "select e from OutboxEvent e order by id", OutboxEvent.class );
		query.setMaxResults( batchSize );
		return query.list();
	}

	private static int createOutboxRetries(Session session,
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

	private static int deleteOutboxes(Session session, List<Integer> ids) {
		return withinTransaction( session, () -> {
			Query<?> query = session.createQuery( "delete from OutboxEvent e where e.id in :ids" );
			query.setParameter( "ids", ids );
			int update = query.executeUpdate();

			session.flush();
			session.clear();
			return update;
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

