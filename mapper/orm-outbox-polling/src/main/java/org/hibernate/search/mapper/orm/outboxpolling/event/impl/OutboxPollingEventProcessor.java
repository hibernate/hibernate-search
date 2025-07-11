/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.common.spi.SessionHelper;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class OutboxPollingEventProcessor implements ToStringTreeAppendable {

	public static String namePrefix(String tenantId) {
		StringBuilder prefix = new StringBuilder( "Outbox event processor" );
		if ( tenantId != null ) {
			prefix.append( " - Tenant <" ).append( tenantId ).append( ">" );
		}
		return prefix.toString();
	}

	private static final ConfigurationProperty<Integer> POLLING_INTERVAL =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_POLLING_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault(
							HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PULSE_INTERVAL =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PULSE_EXPIRATION =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_PULSE_EXPIRATION )
					.asIntegerStrictlyPositive()
					.withDefault(
							HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION )
					.build();

	private static final ConfigurationProperty<Integer> BATCH_SIZE =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_BATCH_SIZE )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_BATCH_SIZE )
					.build();

	private static final OptionalConfigurationProperty<Integer> TRANSACTION_TIMEOUT =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_TRANSACTION_TIMEOUT )
					.asIntegerStrictlyPositive()
					.build();

	private static final ConfigurationProperty<Integer> RETRY_DELAY =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_RETRY_DELAY )
					.asIntegerPositiveOrZero()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_RETRY_DELAY )
					.build();

	private static final ConfigurationProperty<Integer> EVENT_PROCESSOR_EVENT_LOCK_RETRY_MAX =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_EVENT_LOCK_RETRY_MAX )
					.asIntegerPositiveOrZero()
					.withDefault(
							HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_EVENT_LOCK_RETRY_MAX )
					.build();

	private static final ConfigurationProperty<Integer> EVENT_PROCESSOR_EVENT_LOCK_RETRY_DELAY =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_EVENT_LOCK_RETRY_DELAY )
					.asIntegerPositiveOrZero()
					.withDefault(
							HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_EVENT_LOCK_RETRY_DELAY )
					.build();

	public static Factory factory(AutomaticIndexingMappingContext mapping, Clock clock, String tenantId,
			ConfigurationPropertySource configurationSource) {
		OutboxEventLoader loader = new OutboxEventLoader( mapping.sessionFactory().getJdbcServices().getDialect() );

		Duration pollingInterval = POLLING_INTERVAL.getAndTransform( configurationSource, Duration::ofMillis );
		Duration pulseInterval = PULSE_INTERVAL.getAndTransform( configurationSource,
				v -> OutboxConfigUtils.checkPulseInterval( Duration.ofMillis( v ), pollingInterval ) );
		Duration pulseExpiration = PULSE_EXPIRATION.getAndTransform( configurationSource,
				v -> OutboxConfigUtils.checkPulseExpiration( Duration.ofMillis( v ), pulseInterval ) );

		int batchSize = BATCH_SIZE.get( configurationSource );
		int retryDelay = RETRY_DELAY.get( configurationSource );
		Integer transactionTimeout = TRANSACTION_TIMEOUT.get( configurationSource )
				.orElse( null );

		int lockEventsMaxRetry = EVENT_PROCESSOR_EVENT_LOCK_RETRY_MAX.get( configurationSource );
		long lockEventsInterval =
				Duration.ofSeconds( EVENT_PROCESSOR_EVENT_LOCK_RETRY_DELAY.get( configurationSource ) ).toMillis();

		return new Factory( mapping, clock, tenantId, loader, pollingInterval, pulseInterval, pulseExpiration,
				batchSize, retryDelay, transactionTimeout, lockEventsMaxRetry, lockEventsInterval );
	}

	public static class Factory {
		private final AutomaticIndexingMappingContext mapping;
		private final Clock clock;
		private final String tenantId;
		private final OutboxEventLoader loader;
		private final Duration pollingInterval;
		private final Duration pulseInterval;
		private final Duration pulseExpiration;
		private final int batchSize;
		private final int retryDelay;
		private final Integer transactionTimeout;
		private final int lockEventsMaxRetry;
		private final long lockEventsInterval;

		private Factory(AutomaticIndexingMappingContext mapping, Clock clock, String tenantId,
				OutboxEventLoader loader, Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration,
				int batchSize, int retryDelay, Integer transactionTimeout,
				int lockEventsMaxRetry, long lockEventsInterval) {
			this.mapping = mapping;
			this.clock = clock;
			this.tenantId = tenantId;
			this.loader = loader;
			this.pollingInterval = pollingInterval;
			this.pulseInterval = pulseInterval;
			this.pulseExpiration = pulseExpiration;
			this.batchSize = batchSize;
			this.retryDelay = retryDelay;
			this.transactionTimeout = transactionTimeout;
			this.lockEventsMaxRetry = lockEventsMaxRetry;
			this.lockEventsInterval = lockEventsInterval;
		}

		public OutboxPollingEventProcessor create(ScheduledExecutorService scheduledExecutor,
				OutboxEventFinderProvider finderProvider, AgentRepositoryProvider agentRepositoryProvider,
				ShardAssignmentDescriptor shardAssignmentOrNull) {
			String agentName = namePrefix( tenantId )
					+ ( shardAssignmentOrNull == null ? "" : " - " + shardAssignmentOrNull.assignedShardIndex );
			OutboxPollingEventProcessorClusterLink clusterLink = new OutboxPollingEventProcessorClusterLink(
					agentName, mapping.failureHandler(), clock,
					new ShardAssignment.Provider( finderProvider ), pollingInterval, pulseInterval, pulseExpiration,
					shardAssignmentOrNull );

			return new OutboxPollingEventProcessor( agentName, this, scheduledExecutor,
					agentRepositoryProvider, clusterLink );
		}
	}

	private enum Status {
		STOPPED,
		STARTED
	}

	private final String name;
	private final AutomaticIndexingMappingContext mapping;
	private final OutboxEventLoader loader;
	private final long pollingInterval;
	private final int batchSize;
	private final int retryDelay;
	private final int lockEventsMaxRetry;
	private final long lockEventsInterval;

	private final AtomicReference<Status> status = new AtomicReference<>( Status.STOPPED );
	private final OutboxPollingEventProcessorClusterLink clusterLink;
	private final TransactionHelper transactionHelper;
	private final SessionHelper sessionHelper;
	private final AgentClusterLinkContextProvider clusterLinkContextProvider;
	private final FailureHandler failureHandler;
	private final Worker worker;
	private final SingletonTask processingTask;

	public OutboxPollingEventProcessor(String name, Factory factory,
			ScheduledExecutorService executor,
			AgentRepositoryProvider agentRepositoryProvider,
			OutboxPollingEventProcessorClusterLink clusterLink) {
		this.name = name;
		this.mapping = factory.mapping;
		Object tenantId = mapping.tenancyConfiguration().convert( factory.tenantId );
		this.loader = factory.loader;
		this.pollingInterval = factory.pollingInterval.toMillis();
		this.batchSize = factory.batchSize;
		this.retryDelay = factory.retryDelay;
		this.lockEventsMaxRetry = factory.lockEventsMaxRetry;
		this.lockEventsInterval = factory.lockEventsInterval;
		this.clusterLink = clusterLink;

		transactionHelper = new TransactionHelper( mapping.sessionFactory(), factory.transactionTimeout );
		sessionHelper = new SessionHelper( mapping.sessionFactory(), tenantId );
		this.clusterLinkContextProvider = new AgentClusterLinkContextProvider( transactionHelper, sessionHelper,
				agentRepositoryProvider );

		failureHandler = mapping.failureHandler();
		this.worker = new Worker();
		processingTask = new SingletonTask(
				name,
				worker,
				new Scheduler( executor ),
				failureHandler
		);
	}

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "name", name )
				.attribute( "loader", loader )
				.attribute( "pollingInterval", pollingInterval )
				.attribute( "batchSize", batchSize )
				.attribute( "retryDelay", retryDelay )
				.attribute( "clusterLink", clusterLink );
	}

	public void start() {
		OutboxPollingEventsLog.INSTANCE.startingOutboxEventProcessor( name, this );
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
		OutboxPollingEventsLog.INSTANCE.stoppingOutboxEventProcessor( name );
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SingletonTask::stop, processingTask );
			closer.push( OutboxPollingEventProcessor::leaveCluster, this );
		}
	}

	private void leaveCluster() {
		clusterLinkContextProvider.inTransaction( clusterLink::leaveCluster );
	}

	private class Worker implements SingletonTask.Worker {

		private volatile OutboxPollingEventProcessingInstructions instructions;
		private volatile boolean lastExecutionProcessedEvents;

		@Override
		public CompletableFuture<?> work() {
			lastExecutionProcessedEvents = false;

			if ( instructions == null || !instructions.isStillValid() ) {
				// Never perform event processing in the same transaction as a pulse,
				// to reduce transaction contention.
				instructions = clusterLinkContextProvider.inTransaction( clusterLink::pulse );
			}

			// Optimization: don't even try to open a transaction/session for event processing
			// if we know it's not necessary.
			if ( !instructions.eventFinder.isPresent() ) {
				// Processing is disabled for the time being.
				// We will try again later (complete() will be called, re-scheduling the polling for later).
				return CompletableFuture.completedFuture( null );
			}

			try ( SessionImplementor session = sessionHelper.openSession() ) {
				final OutboxEventProcessingPlan eventProcessing = new OutboxEventProcessingPlan( mapping, session );
				transactionHelper.inTransaction( session, () -> {
					Optional<OutboxEventFinder> eventFinder = instructions.eventFinder;
					if ( !eventFinder.isPresent() ) {
						// Processing is disabled for the time being.
						// Don't do anything, we'll try again later
						// (complete() will be called, re-scheduling the polling for later)
						return;
					}

					List<OutboxEvent> events;
					try {
						events = eventFinder.get().findOutboxEvents( session, batchSize );
						if ( events.isEmpty() ) {
							// Nothing to do, try again later (complete() will be called, re-scheduling the polling for later)
							return;
						}
					}
					catch (PessimisticLockException | OptimisticLockException lockException) {
						// Note OptimisticLockException is sometimes (always?) thrown to indicate a *pessimistic* lock failure.
						// It can happen with some databases (CockroachDB in particular, perhaps others)
						// that treat transaction failures as a matter of course that applications should deal with.
						// See also https://www.cockroachlabs.com/docs/v23.1/transaction-retry-error-reference.html
						OutboxPollingEventsLog.INSTANCE.eventProcessorFindEventsUnableToLock( name, lockException );
						return;
					}

					// There are events to process
					lastExecutionProcessedEvents = true;
					// Make sure we will process the next batch ASAP
					// Since we set lastExecutionProcessedEvents to true,
					// calling ensureScheduled() will lead to immediate re-execution right after we're done.
					// See the Scheduler class below.
					ensureScheduled();

					OutboxPollingEventsLog.INSTANCE.processingOutboxEvents( events.size(), name, events );

					// Process the events
					eventProcessing.processEvents( events );
				} );

				// Updating events involves database locks on a table that
				// can see heavily concurrent access (the outbox table),
				// so we do that in a separate transaction, one that is as short as possible.
				OutboxEventUpdater eventUpdater = new OutboxEventUpdater(
						failureHandler, loader, eventProcessing, session, name, retryDelay );
				// We potentially perform this update in multiple transactions,
				// each loading as many events as possible using SKIP_LOCKED,
				// to only load events that are not already locked by another processor.
				// This is to avoid problems related to lock escalation in MS SQL for example,
				// where another processor could be locking on our own events because
				// it locked a page instead of just a row.
				// For more information, see
				// org.hibernate.search.mapper.orm.outboxpolling.impl.OutboxEventLoader.tryLoadLocking
				int retryCount = 0;
				while ( true ) {
					if ( retryCount > lockEventsMaxRetry ) {
						OutboxPollingEventsLog.INSTANCE.eventLockingRetryLimitReached( clusterLink.selfReference(),
								lockEventsMaxRetry, eventUpdater.eventsToProcess() );
						// not failing to not produce an error log:
						return CompletableFuture.completedFuture( null );
					}
					transactionHelper.inTransaction( session, eventUpdater::process );
					if ( eventUpdater.thereAreStillEventsToProcess() ) {
						try {
							Thread.sleep( lockEventsInterval );
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return CompletableFuture.failedFuture( e );
						}
						retryCount++;
					}
					else {
						break;
					}
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

	private class Scheduler implements SingletonTask.Scheduler {
		private final ScheduledExecutorService delegate;

		private Scheduler(ScheduledExecutorService delegate) {
			this.delegate = delegate;
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			OutboxPollingEventProcessingInstructions instructions = worker.instructions;
			if ( instructions == null ) {
				// Before the first pulse (i.e. on startup),
				// execute the worker only after the polling interval.
				// This is to mitigate the impact of infinite loops when there is an unhandled
				// failure while getting instructions (e.g. if the database is not up and running)
				// TODO Ideally we should record unhandled failures in a variable and force a wait after they happen.
				return delegate.schedule( runnable, pollingInterval, TimeUnit.MILLISECONDS );
			}
			else if ( instructions.eventFinder.isPresent() ) {
				if ( worker.lastExecutionProcessedEvents ) {
					// When running and there might be  more events to process,
					// re-execute the worker immediately.
					return delegate.submit( runnable );
				}
				else {
					// When running and there are no more events to process,
					// re-execute the worker after the polling interval.
					return delegate.schedule( runnable, pollingInterval, TimeUnit.MILLISECONDS );
				}
			}
			else {
				// On rebalancing or when suspended,
				// re-execute the worker only after the current instructions expire.
				// Instructions are expected to provide a reasonably high waiting time
				// between when they are issued and when they expire,
				// i.e. at least the polling interval,
				// to avoid polling the database continuously.
				return delegate.schedule( runnable, instructions.timeInMillisecondsToExpiration(), TimeUnit.MILLISECONDS );
			}
		}
	}
}
