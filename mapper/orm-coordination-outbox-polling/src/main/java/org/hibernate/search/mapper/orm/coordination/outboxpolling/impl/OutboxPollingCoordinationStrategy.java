/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationConfigurationContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyPreStopContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.DefaultAgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingEventProcessor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingMassIndexerAgent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventSendingPlan;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.mapping.impl.OutboxPollingSearchMappingImpl;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgent;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingCoordinationStrategy implements CoordinationStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> EVENT_PROCESSOR_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_ENABLED )
					.build();

	private static final OptionalConfigurationProperty<Integer> EVENT_PROCESSOR_SHARDS_TOTAL_COUNT =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_TOTAL_COUNT )
					.asIntegerStrictlyPositive()
					.build();

	private static final OptionalConfigurationProperty<List<Integer>> EVENT_PROCESSOR_SHARDS_ASSIGNED =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_SHARDS_ASSIGNED )
					.asIntegerPositiveOrZero()
					.multivalued()
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends AgentRepositoryProvider>> AGENT_REPOSITORY_PROVIDER =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingImplSettings.CoordinationRadicals.AGENT_REPOSITORY_PROVIDER )
					.asBeanReference( AgentRepositoryProvider.class )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends OutboxEventFinderProvider>> OUTBOX_EVENT_FINDER_PROVIDER =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingImplSettings.CoordinationRadicals.OUTBOX_EVENT_FINDER_PROVIDER )
					.asBeanReference( OutboxEventFinderProvider.class )
					.build();

	private BeanHolder<? extends OutboxEventFinderProvider> finderProviderHolder;
	private BeanHolder<? extends AgentRepositoryProvider> agentRepositoryProviderHolder;

	private TenancyConfiguration tenancyConfiguration;
	private final Map<String, TenantDelegate> tenantDelegates = new LinkedHashMap<>();
	private OutboxPollingSearchMappingImpl outboxPollingSearchMapping;

	@Override
	public void configure(CoordinationConfigurationContext context) {
		context.mappingProducer( new OutboxPollingOutboxEventAdditionalJaxbMappingProducer() );
		context.mappingProducer( new OutboxPollingAgentAdditionalJaxbMappingProducer() );
		context.sendIndexingEventsTo( ctx -> new OutboxPollingOutboxEventSendingPlan( ctx.session() ), true );
	}

	@Override
	public CompletableFuture<?> start(CoordinationStrategyStartContext context) {
		Optional<BeanHolder<? extends AgentRepositoryProvider>> agentRepositoryProviderHolderOptional =
				AGENT_REPOSITORY_PROVIDER.getAndMap(
						context.configurationPropertySource(), context.beanResolver()::resolve );
		if ( agentRepositoryProviderHolderOptional.isPresent() ) {
			agentRepositoryProviderHolder = agentRepositoryProviderHolderOptional.get();
			log.debugf(
					"Outbox processing will use custom agent repository provider '%s'.",
					agentRepositoryProviderHolder.get()
			);
		}
		else {
			agentRepositoryProviderHolder = BeanHolder.of( new DefaultAgentRepository.Provider() );
		}

		Optional<BeanHolder<? extends OutboxEventFinderProvider>> finderProviderHolderOptional =
				OUTBOX_EVENT_FINDER_PROVIDER.getAndMap(
						context.configurationPropertySource(), context.beanResolver()::resolve );
		if ( finderProviderHolderOptional.isPresent() ) {
			finderProviderHolder = finderProviderHolderOptional.get();
			log.debugf(
					"Outbox processing will use custom outbox event finder provider '%s'.",
					finderProviderHolder.get()
			);
		}
		else {
			finderProviderHolder = BeanHolder.of( new DefaultOutboxEventFinder.Provider() );
		}

		tenancyConfiguration = context.tenancyConfiguration();
		Set<String> tenantIds = tenancyConfiguration.tenantIdsOrFail();

		if ( tenantIds.isEmpty() ) {
			// Single-tenant
			TenantDelegate tenantDelegate = new TenantDelegate( null );
			tenantDelegates.put( null, tenantDelegate );
			tenantDelegate.start( context, context.configurationPropertySource() );
		}
		else {
			// Multi-tenant
			for ( String tenantId : tenantIds ) {
				TenantDelegate tenantDelegate = new TenantDelegate( tenantId );
				tenantDelegates.put( tenantId, tenantDelegate );
				ConfigurationPropertySource configurationSource = context.configurationPropertySource();
				configurationSource = configurationSource
						.withMask( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.TENANTS )
						.withMask( tenantId )
						.withFallback( configurationSource );
				tenantDelegate.start( context, configurationSource );
			}
		}

		outboxPollingSearchMapping = new OutboxPollingSearchMappingImpl( context, tenantIds );
		return CompletableFuture.completedFuture( null );
	}
	@Override
	public PojoMassIndexerAgent createMassIndexerAgent(PojoMassIndexerAgentCreateContext context) {
		return tenantDelegate( context.tenantIdentifier() ).massIndexerAgentFactory
				.create( context, agentRepositoryProviderHolder.get() );
	}

	private TenantDelegate tenantDelegate(String tenantId) {
		TenantDelegate tenantDelegate = tenantDelegates.get( tenantId );
		if ( tenantDelegate == null ) {
			throw tenancyConfiguration.invalidTenantId( tenantId );
		}
		return tenantDelegate;
	}

	@Override
	public CompletableFuture<?> completion() {
		List<CompletableFuture<?>> futures = new ArrayList<>();
		for ( TenantDelegate tenantDelegate : tenantDelegates.values() ) {
			if ( tenantDelegate.eventProcessors == null ) {
				continue;
			}
			for ( OutboxPollingEventProcessor eventProcessor : tenantDelegate.eventProcessors ) {
				futures.add( eventProcessor.completion() );
			}
		}
		return CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[0] ) );
	}

	@Override
	public CompletableFuture<?> preStop(CoordinationStrategyPreStopContext context) {
		List<CompletableFuture<?>> futures = new ArrayList<>();
		for ( TenantDelegate tenantDelegate : tenantDelegates.values() ) {
			if ( tenantDelegate.eventProcessors == null ) {
				continue;
			}
			for ( OutboxPollingEventProcessor eventProcessor : tenantDelegate.eventProcessors ) {
				futures.add( eventProcessor.preStop() );
			}
		}
		return CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[0] ) );
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			for ( TenantDelegate tenantDelegate : tenantDelegates.values() ) {
				closer.pushAll( OutboxPollingEventProcessor::stop, tenantDelegate.eventProcessors );
				closer.push( ScheduledExecutorService::shutdownNow, tenantDelegate.eventProcessorExecutor );
			}
			closer.push( BeanHolder::close, finderProviderHolder );
			closer.push( BeanHolder::close, agentRepositoryProviderHolder );
		}
	}

	public OutboxPollingSearchMappingImpl outboxPollingSearchMapping() {
		return outboxPollingSearchMapping;
	}

	private class TenantDelegate {
		private final String tenantId;

		private ScheduledExecutorService eventProcessorExecutor;
		private List<OutboxPollingEventProcessor> eventProcessors;
		private OutboxPollingMassIndexerAgent.Factory massIndexerAgentFactory;

		private TenantDelegate(String tenantId) {
			this.tenantId = tenantId;
		}

		void start(CoordinationStrategyStartContext context, ConfigurationPropertySource configurationSource) {
			if ( EVENT_PROCESSOR_ENABLED.get( configurationSource ) ) {
				initializeEventProcessors( context, configurationSource );
			}
			else {
				log.eventProcessorDisabled( tenantId );
			}

			this.massIndexerAgentFactory = OutboxPollingMassIndexerAgent.factory( context.mapping(), context.clock(),
					tenantId, configurationSource );
		}

		private void initializeEventProcessors(CoordinationStrategyStartContext context,
				ConfigurationPropertySource configurationSource) {
			OutboxPollingEventProcessor.Factory factory = OutboxPollingEventProcessor.factory( context.mapping(),
					context.clock(), tenantId, configurationSource );

			boolean shardsStatic = EVENT_PROCESSOR_SHARDS_TOTAL_COUNT.get( configurationSource ).isPresent()
					|| EVENT_PROCESSOR_SHARDS_ASSIGNED.get( configurationSource ).isPresent();
			List<ShardAssignmentDescriptor> shardAssignmentOrNulls;
			if ( shardsStatic ) {
				int totalShardCount = EVENT_PROCESSOR_SHARDS_TOTAL_COUNT.getAndMapOrThrow(
						configurationSource,
						this::checkTotalShardCount,
						() -> log.missingPropertyForStaticSharding(
								EVENT_PROCESSOR_SHARDS_ASSIGNED.resolveOrRaw( configurationSource ) )
				);
				shardAssignmentOrNulls = EVENT_PROCESSOR_SHARDS_ASSIGNED.getAndMapOrThrow(
						configurationSource,
						shardIndices -> toStaticShardAssignments( configurationSource, totalShardCount, shardIndices ),
						() -> log.missingPropertyForStaticSharding(
								EVENT_PROCESSOR_SHARDS_TOTAL_COUNT.resolveOrRaw( configurationSource ) )
				);
			}
			else {
				shardAssignmentOrNulls = Collections.singletonList( null );
			}

			eventProcessorExecutor = context.threadPoolProvider()
					.newScheduledExecutor( shardAssignmentOrNulls.size(),
							OutboxPollingEventProcessor.namePrefix( tenantId ) );
			eventProcessors = new ArrayList<>();
			for ( ShardAssignmentDescriptor shardAssignmentOrNull : shardAssignmentOrNulls ) {
				eventProcessors.add( factory.create( eventProcessorExecutor, finderProviderHolder.get(),
						agentRepositoryProviderHolder.get(), shardAssignmentOrNull ) );
			}
			for ( OutboxPollingEventProcessor eventProcessor : eventProcessors ) {
				eventProcessor.start();
			}
		}

		private Integer checkTotalShardCount(Integer totalShardCount) {
			if ( totalShardCount <= 0 ) {
				throw log.invalidTotalShardCount();
			}
			return totalShardCount;
		}

		private List<ShardAssignmentDescriptor> toStaticShardAssignments(ConfigurationPropertySource configurationPropertySource,
				int totalShardCount, List<Integer> shardIndices) {
			// Remove duplicates
			Set<Integer> uniqueShardIndices = new HashSet<>( shardIndices );
			for ( Integer shardIndex : uniqueShardIndices ) {
				if ( !( 0 <= shardIndex && shardIndex < totalShardCount ) ) {
					throw log.invalidShardIndex( totalShardCount,
							EVENT_PROCESSOR_SHARDS_TOTAL_COUNT.resolveOrRaw( configurationPropertySource ) );
				}
			}
			List<ShardAssignmentDescriptor> shardAssignment = new ArrayList<>();
			for ( Integer shardIndex : uniqueShardIndices ) {
				shardAssignment.add( new ShardAssignmentDescriptor( totalShardCount, shardIndex ) );
			}
			return shardAssignment;
		}

	}
}
