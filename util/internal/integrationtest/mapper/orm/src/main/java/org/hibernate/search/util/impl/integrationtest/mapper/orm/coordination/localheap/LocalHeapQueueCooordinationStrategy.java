/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.coordination.localheap;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.coordination.common.spi.CooordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyPreStopContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;

public class LocalHeapQueueCooordinationStrategy implements CooordinationStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int MAX_TASKS_PER_BATCH = 10;
	private static final String NAME = "Local heap queue automatic indexing";

	private ThreadPoolExecutor threadPool;
	private BatchingExecutor<LocalHeapQueueProcessor> executor;

	private volatile boolean acceptEvents;

	@Override
	public CompletableFuture<?> start(CoordinationStrategyStartContext context) {
		threadPool = context.threadPoolProvider().newFixedThreadPool( 1, NAME );
		executor = new BatchingExecutor<>( NAME,
				new LocalHeapQueueProcessor( context.mapping() ), MAX_TASKS_PER_BATCH, true,
						context.mapping().failureHandler() );
		executor.start( threadPool );
		acceptEvents = true;
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void configure(CoordinationConfigurationContext context) {
		context.sendIndexingEventsTo( this::createSendingPlan, false );
	}

	private AutomaticIndexingQueueEventSendingPlan createSendingPlan(AutomaticIndexingEventSendingSessionContext context) {
		return new SendingPlan();
	}

	@Override
	public CompletableFuture<?> completion() {
		return executor.completion();
	}

	@Override
	public CompletableFuture<?> preStop(CoordinationStrategyPreStopContext context) {
		acceptEvents = false;
		return executor.completion();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BatchingExecutor::stop, executor );
			closer.push( ThreadPoolExecutor::shutdownNow, threadPool );
		}
	}

	private class SendingPlan implements AutomaticIndexingQueueEventSendingPlan {
		private final List<LocalHeapQueueIndexingEvent> content = new ArrayList<>();

		@Override
		public void append(String entityName, Object identifier, String serializedId,
				PojoIndexingQueueEventPayload payload) {
			checkAcceptsEvents();
			LocalHeapQueueIndexingEvent event = new LocalHeapQueueIndexingEvent( entityName, identifier,
					serializedId, SerializationUtils.serialize( payload ) );
			log.tracef( "Planning to send %s from %s", event, this );
			content.add( event );
		}

		@Override
		public void discard() {
			content.clear();
		}

		@Override
		public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> sendAndReport(
				EntityReferenceFactory<R> entityReferenceFactory) {
			try {
				MultiEntityOperationExecutionReport.Builder<R> builder = MultiEntityOperationExecutionReport.builder();
				log.tracef( "Sending %s from %s", content, this );
				for ( LocalHeapQueueIndexingEvent event : content ) {
					try {
						executor.submit( event );
						checkAcceptsEvents();
					}
					catch (RuntimeException | InterruptedException e) {
						builder.throwable( e );
						builder.failingEntityReference( entityReferenceFactory, event.entityName, event.identifier );
					}
				}
				return CompletableFuture.completedFuture( builder.build() );
			}
			finally {
				content.clear();
			}
		}

		private void checkAcceptsEvents() {
			if ( !acceptEvents ) {
				throw new IllegalStateException( "The automatic indexing strategy is stopping and cannot accept new events" );
			}
		}
	}
}
