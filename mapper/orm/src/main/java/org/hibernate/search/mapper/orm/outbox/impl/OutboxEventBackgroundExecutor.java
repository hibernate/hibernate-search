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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
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
	private final FailureHandler failureHandler;
	private final SingletonTask processingTask;

	public OutboxEventBackgroundExecutor(AutomaticIndexingMappingContext mapping, ScheduledExecutorService executor,
			int pollingInterval, int batchSize) {
		this.mapping = mapping;
		this.pollingInterval = pollingInterval;
		this.batchSize = batchSize;

		failureHandler = mapping.failureHandler();
		processingTask = new SingletonTask(
				"Delayed commit for " + OutboxTableAutomaticIndexingStrategy.NAME,
				new HibernateOrmOutboxWorker(),
				new HibernateOrmOutboxScheduler( executor ),
				failureHandler
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
				return withinTransaction( session, () -> {
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
					OutboxEventProcessingPlan eventProcessing = new OutboxEventProcessingPlan(
							mapping, session, outboxes );
					List<Integer> ids = eventProcessing.processEvents();
					createOutboxRetries( failureHandler, session, eventProcessing );
					deleteOutboxes( session, ids );

					return CompletableFuture.completedFuture( null );
				} );
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

	private List<OutboxEvent> findOutboxes(Session session) {
		Query<OutboxEvent> query = session.createQuery(
				"select e from OutboxEvent e order by e.id", OutboxEvent.class );
		query.setMaxResults( batchSize );
		return query.list();
	}

	private static void createOutboxRetries(FailureHandler failureHandler, Session session,
			OutboxEventProcessingPlan processingPlan) {
		for ( Map.Entry<OutboxEventReference, List<OutboxEvent>> entry : processingPlan.getFailedEvents().entrySet() ) {
			int minRetries = minRetries( entry.getValue() );
			if ( minRetries >= MAX_RETRIES ) {
				EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
				builder.throwable( log.maxRetryExhausted( MAX_RETRIES ) );
				builder.failingOperation( "Processing an outbox event." );
				builder.entityReference( processingPlan.entityReference( entry.getKey() ) );
				failureHandler.handle( builder.build() );
				continue;
			}
			OutboxEvent eventRetry = new OutboxEvent(
					mergeTypes( entry.getValue() ), entry.getKey().getEntityName(), entry.getKey().getEntityId(),
					mergeDocumentRoutes( entry.getValue() ),
					minRetries + 1
			);
			session.persist( eventRetry );
		}
	}

	private static int minRetries(List<OutboxEvent> events) {
		int min = MAX_RETRIES;
		for ( OutboxEvent event : events ) {
			min = Math.min( event.getRetries(), min );
		}
		return min;
	}

	private static void deleteOutboxes(Session session, List<Integer> ids) {
		Query<?> query = session.createQuery( "delete from OutboxEvent e where e.id in :ids" );
		query.setParameter( "ids", ids );
		query.executeUpdate();

		session.flush();
		session.clear();
	}

	private static byte[] mergeDocumentRoutes(List<OutboxEvent> events) {
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

	private static OutboxEvent.Type mergeTypes(List<OutboxEvent> events) {
		for ( OutboxEvent event : events ) {
			if ( !OutboxEvent.Type.DELETE.equals( event.getType() ) ) {
				return OutboxEvent.Type.ADD_OR_UPDATE;
			}
		}
		return OutboxEvent.Type.DELETE;
	}
}

