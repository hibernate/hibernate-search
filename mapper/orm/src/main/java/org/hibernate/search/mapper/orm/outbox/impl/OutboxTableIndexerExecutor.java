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

public class OutboxTableIndexerExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final int MAX_BATCH_SIZE = 50;

	private final AutomaticIndexingMappingContext mapping;
	private final ScheduledExecutorService executor;

	public OutboxTableIndexerExecutor(AutomaticIndexingMappingContext mapping,
			ScheduledExecutorService executor) {
		this.mapping = mapping;
		this.executor = executor;
	}

	public void start() {
		executor.scheduleAtFixedRate( this::run, 0, 8, TimeUnit.MILLISECONDS );
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
		try ( Session session = mapping.sessionFactory().openSession() ) {
			List<OutboxEvent> events = findOutboxEntries( session );

			while ( !events.isEmpty() ) {
				OutboxEventProcessing<OutboxEvent> eventProcessing = new OutboxEventProcessing<>(
						mapping, session, events );
				List<Integer> ids = eventProcessing.processEvents();

				try {
					createNewOutboxRetrials( session, eventProcessing.getFailedEvents() );
				}
				catch (Throwable throwable) {
					log.errorf( throwable, "Failed to store outbox events for future retrials" );
				}

				try {
					deleteOutboxEntities( session, ids );
				}
				catch (Throwable throwable) {
					log.errorf( throwable, "Failed to delete outbox events from the outbox table" );
				}

				events = findOutboxEntries( session );
			}
		}
	}

	private static List<OutboxEvent> findOutboxEntries(Session session) {
		Query<OutboxEvent> query = session.createQuery( "select e from OutboxEvent e order by id", OutboxEvent.class );
		query.setMaxResults( MAX_BATCH_SIZE );
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

