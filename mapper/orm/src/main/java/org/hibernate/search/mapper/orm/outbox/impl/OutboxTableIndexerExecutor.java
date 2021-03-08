/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

import static org.hibernate.search.mapper.orm.common.impl.TransactionUtils.withinTransaction;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public class OutboxTableIndexerExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final int MAX_BATCH_SIZE = 50;

	private final AutomaticIndexingMappingContext mapping;
	private final Set<Integer> ids = new HashSet<>();
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
				AutomaticIndexingQueueEventProcessingPlan plan = mapping.createIndexingQueueEventProcessingPlan(
						session );

				events = findOutboxEntries( session );
				for ( OutboxEvent event : events ) {
					DocumentRoutesDescriptor routes = getRoutes( event );

					switch ( event.getType() ) {
						case ADD:
							plan.add( event.getEntityName(), event.getEntityId(), routes );
							break;
						case ADD_OR_UPDATE:
							plan.addOrUpdate( event.getEntityName(), event.getEntityId(), routes );
							break;
						case DELETE:
							plan.delete( event.getEntityName(), event.getEntityId(), routes );
							break;
					}
					ids.add( event.getId() );
				}

				AtomicBoolean fail = new AtomicBoolean( false );

				try {
					List<OutboxEvent> finalEvents = events;
					plan.executeAndReport().whenComplete( (report, throwable) -> {
						if ( throwable != null ) {
							for ( OutboxEvent event : finalEvents ) {
								log.errorf( throwable, "Failed to process event '%s'", event );
							}

							fail.set( true );
						}
						else if ( report.throwable().isPresent() ) {
							log.errorf(
									report.throwable().get(), "Failed to reindex entities '%s'",
									report.failingEntityReferences()
							);

							fail.set( true );
						}
					} ).get();
				}
				catch (Throwable throwable) {
					for ( OutboxEvent event : events ) {
						log.errorf( throwable, "Failed to process event '%s'", event );
					}

					fail.set( true );
				}

				if ( fail.get() ) {
					return; // retry the batch in the next execution
				}

				try {
					deleteOutboxEntities( session );
				}
				catch (Throwable throwable) {
					log.errorf( throwable, "Failed to delete outbox events from the outbox table" );
				}

				events = findOutboxEntries( session );
			}
		}
	}

	private static DocumentRoutesDescriptor getRoutes(OutboxEvent event) {
		return SerializationUtils.deserialize( DocumentRoutesDescriptor.class, event.getDocumentRoutes() );
	}

	private List<OutboxEvent> findOutboxEntries(Session session) {
		Query<OutboxEvent> query = session.createQuery( "select e from OutboxEvent e order by id", OutboxEvent.class );
		query.setMaxResults( MAX_BATCH_SIZE );
		return query.list();
	}

	private int deleteOutboxEntities(Session session) {
		return withinTransaction( session, () -> {
			Query query = session.createQuery( "delete from OutboxEvent e where e.id in :ids" );
			query.setParameter( "ids", ids );
			int update = query.executeUpdate();

			session.flush();
			session.clear();
			return update;
		} );
	}
}

