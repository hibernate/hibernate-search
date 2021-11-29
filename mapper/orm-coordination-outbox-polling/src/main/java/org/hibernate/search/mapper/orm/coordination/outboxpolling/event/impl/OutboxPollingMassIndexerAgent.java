/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class OutboxPollingMassIndexerAgent implements PojoMassIndexerAgent {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String NAME_PREFIX = "Mass indexer agent";

	private static final ConfigurationProperty<Integer> POLLING_INTERVAL =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_POLLING_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_POLLING_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PULSE_INTERVAL =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_PULSE_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PULSE_EXPIRATION =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_EXPIRATION )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_PULSE_EXPIRATION )
					.build();

	public static Factory factory(AutomaticIndexingMappingContext mapping, Clock clock,
			ConfigurationPropertySource configurationSource) {
		Duration pollingInterval = POLLING_INTERVAL.getAndTransform( configurationSource, Duration::ofMillis );
		Duration pulseInterval = PULSE_INTERVAL.getAndTransform( configurationSource,
				v -> OutboxConfigUtils.checkPulseInterval( Duration.ofMillis( v ), pollingInterval ) );
		Duration pulseExpiration = PULSE_EXPIRATION.getAndTransform( configurationSource,
				v -> OutboxConfigUtils.checkPulseExpiration( Duration.ofMillis( v ), pulseInterval ) );

		return new Factory( mapping, clock, pollingInterval, pulseInterval, pulseExpiration );
	}

	public static class Factory {
		private final AutomaticIndexingMappingContext mapping;
		private final Clock clock;
		private final Duration pollingInterval;
		private final Duration pulseInterval;
		private final Duration pulseExpiration;

		private Factory(AutomaticIndexingMappingContext mapping, Clock clock,
				Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration) {
			this.mapping = mapping;
			this.clock = clock;
			this.pollingInterval = pollingInterval;
			this.pulseInterval = pulseInterval;
			this.pulseExpiration = pulseExpiration;
		}

		public OutboxPollingMassIndexerAgent create(PojoMassIndexerAgentCreateContext context,
				AgentRepositoryProvider agentRepositoryProvider) {
			String agentName = NAME_PREFIX;
			OutboxPollingMassIndexerAgentClusterLink clusterLink = new OutboxPollingMassIndexerAgentClusterLink(
					agentName, mapping.failureHandler(), clock,
					pollingInterval, pulseInterval, pulseExpiration );

			ScheduledExecutorService executor = context.newScheduledExecutor( 1, agentName );

			return new OutboxPollingMassIndexerAgent( agentName, this, executor,
					agentRepositoryProvider, clusterLink );
		}
	}


	private enum Status {
		STOPPED,
		STARTED
	}

	private final String name;
	private final ScheduledExecutorService executor;
	private final AutomaticIndexingMappingContext mapping;
	private final long pollingInterval;

	private final AtomicReference<Status> status = new AtomicReference<>( Status.STOPPED );
	private final AgentRepositoryProvider agentRepositoryProvider;
	private final OutboxPollingMassIndexerAgentClusterLink clusterLink;
	private final TransactionHelper transactionHelper;
	private final Worker worker;
	private final SingletonTask processingTask;

	private OutboxPollingMassIndexerAgent(String name, Factory factory,
			ScheduledExecutorService executor,
			AgentRepositoryProvider agentRepositoryProvider,
			OutboxPollingMassIndexerAgentClusterLink clusterLink) {
		this.name = name;
		this.executor = executor;
		this.mapping = factory.mapping;
		this.pollingInterval = factory.pollingInterval.toMillis();
		this.agentRepositoryProvider = agentRepositoryProvider;
		this.clusterLink = clusterLink;

		transactionHelper = new TransactionHelper( mapping.sessionFactory() );
		worker = new Worker();
		processingTask = new SingletonTask(
				name,
				worker,
				new Scheduler( executor ),
				mapping.failureHandler()
		);
	}

	@Override
	public CompletableFuture<?> start() {
		log.startingOutboxMassIndexerAgent( name );
		status.set( Status.STARTED );
		processingTask.ensureScheduled();
		return worker.agentFullyStartedFuture;
	}

	@Override
	public CompletableFuture<?> preStop() {
		status.set( Status.STOPPED );
		return processingTask.completion();
	}

	@Override
	public void stop() {
		log.stoppingOutboxMassIndexerAgent( name );
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SingletonTask::stop, processingTask );
			closer.push( ScheduledExecutorService::shutdownNow, executor );
			closer.push( OutboxPollingMassIndexerAgent::leaveCluster, this );
		}
	}

	private void leaveCluster() {
		try ( SessionImplementor session = (SessionImplementor) mapping.sessionFactory().openSession() ) {
			transactionHelper.begin( session, null );
			try {
				AgentRepository agentRepository = agentRepositoryProvider.create( session );
				clusterLink.leaveCluster( agentRepository );
				transactionHelper.commit( session );
			}
			catch (RuntimeException e) {
				transactionHelper.rollbackSafely( session, e );
			}
		}
	}

	private class Worker implements SingletonTask.Worker {

		private volatile OutboxPollingMassIndexingInstructions instructions;
		private final CompletableFuture<?> agentFullyStartedFuture = new CompletableFuture<>();

		@Override
		public CompletableFuture<?> work() {
			// Optimization: don't even try to open a transaction/session if we know it's not necessary.
			if ( instructions != null && instructions.isStillValid() ) {
				// We will try again later (complete() will be called, re-scheduling the polling for later).
				return CompletableFuture.completedFuture( null );
			}

			try ( SessionImplementor session = (SessionImplementor) mapping.sessionFactory().openSession() ) {
				transactionHelper.inTransaction( session, null, s -> {
					if ( instructions == null || !instructions.isStillValid() ) {
						AgentRepository agentRepository = agentRepositoryProvider.create( session );
						instructions = clusterLink.pulse( agentRepository );
						if ( instructions.considerEventProcessingSuspended ) {
							agentFullyStartedFuture.complete( null );
						}
					}
				} );
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
			OutboxPollingMassIndexingInstructions instructions = worker.instructions;
			if ( instructions == null ) {
				// Before the first pulse (i.e. on startup),
				// execute the worker only after the polling interval.
				// This is to mitigate the impact of infinite loops when there is an unhandled
				// failure while getting instructions (e.g. if the database is not up and running)
				// TODO Ideally we should record unhandled failures in a variable and force a wait after they happen.
				return delegate.schedule( runnable, pollingInterval, TimeUnit.MILLISECONDS );
			}
			else {
				// Once we have instructions,
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
