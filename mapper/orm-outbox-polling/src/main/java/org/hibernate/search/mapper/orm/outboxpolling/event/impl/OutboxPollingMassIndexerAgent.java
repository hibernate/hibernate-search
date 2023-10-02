/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingMappingContext;
import org.hibernate.search.mapper.orm.common.spi.SessionHelper;
import org.hibernate.search.mapper.orm.common.spi.TransactionHelper;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentStartContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class OutboxPollingMassIndexerAgent implements PojoMassIndexerAgent, ToStringTreeAppendable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static String name(String tenantId) {
		StringBuilder prefix = new StringBuilder( "Mass indexer agent" );
		if ( tenantId != null ) {
			prefix.append( " - Tenant <" ).append( tenantId ).append( ">" );
		}
		return prefix.toString();
	}

	private static final ConfigurationProperty<Integer> POLLING_INTERVAL =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_POLLING_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_POLLING_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PULSE_INTERVAL =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_PULSE_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PULSE_EXPIRATION =
			ConfigurationProperty
					.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.MASS_INDEXER_PULSE_EXPIRATION )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_MASS_INDEXER_PULSE_EXPIRATION )
					.build();

	public static Factory factory(AutomaticIndexingMappingContext mapping, Clock clock, String tenantId,
			ConfigurationPropertySource configurationSource) {
		Duration pollingInterval = POLLING_INTERVAL.getAndTransform( configurationSource, Duration::ofMillis );
		Duration pulseInterval = PULSE_INTERVAL.getAndTransform( configurationSource,
				v -> OutboxConfigUtils.checkPulseInterval( Duration.ofMillis( v ), pollingInterval ) );
		Duration pulseExpiration = PULSE_EXPIRATION.getAndTransform( configurationSource,
				v -> OutboxConfigUtils.checkPulseExpiration( Duration.ofMillis( v ), pulseInterval ) );

		return new Factory( mapping, clock, tenantId, pollingInterval, pulseInterval, pulseExpiration );
	}

	public static class Factory {
		private final AutomaticIndexingMappingContext mapping;
		private final Clock clock;
		private final String tenantId;
		private final Duration pollingInterval;
		private final Duration pulseInterval;
		private final Duration pulseExpiration;

		private Factory(AutomaticIndexingMappingContext mapping, Clock clock, String tenantId,
				Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration) {
			this.mapping = mapping;
			this.clock = clock;
			this.tenantId = tenantId;
			this.pollingInterval = pollingInterval;
			this.pulseInterval = pulseInterval;
			this.pulseExpiration = pulseExpiration;
		}

		public OutboxPollingMassIndexerAgent create(AgentRepositoryProvider agentRepositoryProvider) {
			String agentName = name( tenantId );
			OutboxPollingMassIndexerAgentClusterLink clusterLink = new OutboxPollingMassIndexerAgentClusterLink(
					agentName, mapping.failureHandler(), clock,
					pollingInterval, pulseInterval, pulseExpiration );

			return new OutboxPollingMassIndexerAgent( agentName, this, agentRepositoryProvider, clusterLink );
		}
	}


	private enum Status {
		STOPPED,
		STARTED
	}

	private final String name;
	private final long pollingInterval;

	private final AtomicReference<Status> status = new AtomicReference<>( Status.STOPPED );
	private final OutboxPollingMassIndexerAgentClusterLink clusterLink;
	private final AgentClusterLinkContextProvider clusterLinkContextProvider;
	private final Worker worker;
	private SingletonTask processingTask;

	private OutboxPollingMassIndexerAgent(String name, Factory factory,
			AgentRepositoryProvider agentRepositoryProvider,
			OutboxPollingMassIndexerAgentClusterLink clusterLink) {
		this.name = name;
		AutomaticIndexingMappingContext mapping = factory.mapping;
		this.pollingInterval = factory.pollingInterval.toMillis();
		Object tenantId = mapping.tenancyConfiguration().convert( factory.tenantId );
		this.clusterLink = clusterLink;

		TransactionHelper transactionHelper = new TransactionHelper( mapping.sessionFactory(), null );
		SessionHelper sessionHelper = new SessionHelper( mapping.sessionFactory(), tenantId );
		this.clusterLinkContextProvider = new AgentClusterLinkContextProvider( transactionHelper, sessionHelper,
				agentRepositoryProvider );

		this.worker = new Worker();
	}

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "name", name )
				.attribute( "pollingInterval", pollingInterval )
				.attribute( "clusterLink", clusterLink );
	}

	@Override
	public CompletableFuture<?> start(PojoMassIndexerAgentStartContext context) {
		log.startingOutboxMassIndexerAgent( name, this );

		processingTask = new SingletonTask(
				name,
				worker,
				new Scheduler( context.scheduledExecutor() ),
				context.failureHandler()
		);

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
			closer.push( OutboxPollingMassIndexerAgent::leaveCluster, this );
		}
	}

	private void leaveCluster() {
		clusterLinkContextProvider.inTransaction( clusterLink::leaveCluster );
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

			instructions = clusterLinkContextProvider.inTransaction( clusterLink::pulse );
			if ( instructions.considerEventProcessingSuspended ) {
				agentFullyStartedFuture.complete( null );
			}

			return CompletableFuture.completedFuture( null );
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
