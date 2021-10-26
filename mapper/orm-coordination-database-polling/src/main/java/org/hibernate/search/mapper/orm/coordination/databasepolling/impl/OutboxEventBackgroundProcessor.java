/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.OptimisticLockException;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxEventBackgroundProcessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private enum Status {
		STOPPED,
		STARTED
	}

	private final String name;
	private final AutomaticIndexingMappingContext mapping;
	private final OutboxEventFinder finder;
	private final int pollingInterval;
	private final int batchSize;
	private final Integer transactionTimeout;
	private final AtomicReference<Status> status = new AtomicReference<>( Status.STOPPED );
	private final FailureHandler failureHandler;
	private final SingletonTask processingTask;

	public OutboxEventBackgroundProcessor(String name,
			AutomaticIndexingMappingContext mapping, ScheduledExecutorService executor,
			OutboxEventFinder finder,
			int pollingInterval, int batchSize,
			Integer transactionTimeout) {
		this.name = name;
		this.mapping = mapping;
		this.finder = finder;
		this.pollingInterval = pollingInterval;
		this.batchSize = batchSize;
		this.transactionTimeout = transactionTimeout;

		failureHandler = mapping.failureHandler();
		processingTask = new SingletonTask(
				name,
				new DatabasePollingOutboxWorker( mapping.sessionFactory() ),
				new DatabasePollingHibernateOrmOutboxScheduler( executor ),
				failureHandler
		);
	}

	public void start() {
		log.startingOutboxEventProcessor( name );
		status.set( Status.STARTED );
		processingTask.ensureScheduled();
	}

	public CompletableFuture<?> completion() {
		return processingTask.completion();
	}

	public CompletableFuture<?> preStop() {
		status.set( Status.STOPPED );
		return processingTask.completion();
	}

	public void stop() {
		log.stoppingOutboxEventProcessor( name );
		processingTask.stop();
	}

	private class DatabasePollingOutboxWorker implements SingletonTask.Worker {

		private final TransactionHelper transactionHelper;

		public DatabasePollingOutboxWorker(SessionFactoryImplementor sessionFactory) {
			transactionHelper = new TransactionHelper( sessionFactory );
		}

		@Override
		public CompletableFuture<?> work() {
			if ( mapping.sessionFactory().isClosed() ) {
				// Work around HHH-14541, which is not currently fixed in ORM 5.4.
				// Even if a fix gets backported, the bug will still be present in older 5.4 versions,
				// so we'd better keep this workaround.
				log.sessionFactoryIsClosedOnOutboxProcessing();
				return CompletableFuture.completedFuture( null );
			}

			try ( SessionImplementor session = (SessionImplementor) mapping.sessionFactory().openSession() ) {
				final OutboxEventProcessingPlan eventProcessing = new OutboxEventProcessingPlan( mapping, session );
				transactionHelper.inTransaction( session, transactionTimeout, s -> {
					List<OutboxEvent> events;
					try {
						events = finder.findOutboxEvents( session, batchSize );
					}
					catch (OptimisticLockException lockException) {
						// Don't be fooled by the exception type, this is actually a *pessimistic* lock failure.
						// It can happen with some databases (Mariadb before 10.6, perhaps others) that do not support
						// skipping locked rows (see LockOptions.SKIP).
						// If that happens, we will just log something and try again later.
						// See also https://jira.mariadb.org/browse/MDEV-13115
						log.outboxEventProcessorUnableToLock( name, lockException );
						events = Collections.emptyList();
					}
					if ( events.isEmpty() ) {
						// Nothing to do, try again later (complete() will be called, re-scheduling the polling for later)
						return;
					}

					// There are events to process
					// Make sure we will process the next batch ASAP
					// Since the worker is already working,
					// calling ensureScheduled() will lead to immediate re-execution right after we're done
					ensureScheduled();

					log.tracef( "Processing %d outbox events for '%s': '%s'", events.size(), name, events );

					// Process the events
					eventProcessing.processEvents( events );
				} );

				OutboxEventUpdater eventUpdater = new OutboxEventUpdater(
						failureHandler, eventProcessing, session, name );
				while ( eventUpdater.thereAreStillEventsToProcess() ) {
					transactionHelper.inTransaction( session, transactionTimeout, s -> eventUpdater.process() );
				}

				return CompletableFuture.completedFuture( null );
			}
		}

		@Override
		public void complete() {
			// Make sure we poll again in a few seconds.
			// Since the worker is no longer working at this point,
			// calling ensureScheduled() will lead to delayed re-execution.
			ensureScheduled();
		}

		private void ensureScheduled() {
			// Only schedule the task while the Hibernate Search is started;
			// as soon as Hibernate Search stops,
			// we will finish processing the current batch of events and leave
			// the remaining events to be processed when the application restarts.
			if ( status.get() == Status.STARTED ) {
				processingTask.ensureScheduled();
			}
		}
	}

	private class DatabasePollingHibernateOrmOutboxScheduler implements SingletonTask.Scheduler {
		private final ScheduledExecutorService delegate;

		private DatabasePollingHibernateOrmOutboxScheduler(ScheduledExecutorService delegate) {
			this.delegate = delegate;
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			return delegate.schedule( runnable, pollingInterval, TimeUnit.MILLISECONDS );
		}
	}
}
