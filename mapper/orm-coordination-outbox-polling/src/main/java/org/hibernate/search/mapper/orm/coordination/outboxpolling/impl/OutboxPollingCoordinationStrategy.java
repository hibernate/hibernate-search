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
import java.util.List;
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
import org.hibernate.search.mapper.orm.coordination.common.spi.CooordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyPreStopContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyStartContext;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl.HibernateOrmMapperOutboxPollingImplSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.DefaultAgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingOutboxEventSendingPlan;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxPollingEventProcessor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingCoordinationStrategy implements CooordinationStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> SHARDS_STATIC =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.SHARDS_STATIC )
					.asBoolean()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_SHARDS_STATIC )
					.build();

	private static final OptionalConfigurationProperty<Integer> SHARDS_TOTAL_COUNT =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.SHARDS_TOTAL_COUNT )
					.asIntegerStrictlyPositive()
					.build();

	private static final OptionalConfigurationProperty<List<Integer>> SHARDS_ASSIGNED =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.SHARDS_ASSIGNED )
					.asIntegerPositiveOrZero()
					.multivalued()
					.build();

	private static final ConfigurationProperty<Boolean> EVENT_PROCESSOR_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingSettings.CoordinationRadicals.EVENT_PROCESSOR_ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperOutboxPollingSettings.Defaults.COORDINATION_EVENT_PROCESSOR_ENABLED )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends AgentRepositoryProvider>> EVENT_PROCESSOR_AGENT_REPOSITORY_PROVIDER =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingImplSettings.CoordinationRadicals.EVENT_PROCESSOR_AGENT_REPOSITORY_PROVIDER )
					.asBeanReference( AgentRepositoryProvider.class )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends OutboxEventFinderProvider>> EVENT_PROCESSOR_OUTBOX_EVENT_FINDER_PROVIDER =
			ConfigurationProperty.forKey( HibernateOrmMapperOutboxPollingImplSettings.CoordinationRadicals.EVENT_PROCESSOR_OUTBOX_EVENT_FINDER_PROVIDER )
					.asBeanReference( OutboxEventFinderProvider.class )
					.build();

	private BeanHolder<? extends OutboxEventFinderProvider> finderProviderHolder;
	private BeanHolder<? extends AgentRepositoryProvider> agentRepositoryProviderHolder;
	private ScheduledExecutorService scheduledExecutor;
	private List<OutboxPollingEventProcessor> eventProcessors;

	@Override
	public void configure(CoordinationConfigurationContext context) {
		context.mappingProducer( new OutboxPollingOutboxEventAdditionalJaxbMappingProducer() );
		context.mappingProducer( new OutboxPollingAgentAdditionalJaxbMappingProducer() );
		context.sendIndexingEventsTo( ctx -> new OutboxPollingOutboxEventSendingPlan( ctx.session() ), true );
	}

	@Override
	public CompletableFuture<?> start(CoordinationStrategyStartContext context) {
		Optional<BeanHolder<? extends AgentRepositoryProvider>> agentRepositoryProviderHolderOptional =
				EVENT_PROCESSOR_AGENT_REPOSITORY_PROVIDER.getAndMap(
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
				EVENT_PROCESSOR_OUTBOX_EVENT_FINDER_PROVIDER.getAndMap(
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

		ConfigurationPropertySource configurationSource = context.configurationPropertySource();

		if ( EVENT_PROCESSOR_ENABLED.get( configurationSource ) ) {
			initializeEventProcessors( context );
		}
		else {
			// IMPORTANT: in this case we don't even configure sharding, and that's on purpose.
			// Application developers may want a fixed number of processing nodes (static sharding)
			// with a varying number of non-processing nodes,
			// so we want to make those non-processing nodes easy to configure.
			log.eventProcessorDisabled();
		}

		return CompletableFuture.completedFuture( null );
	}

	private void initializeEventProcessors(CoordinationStrategyStartContext context) {
		ConfigurationPropertySource configurationSource = context.configurationPropertySource();

		// IMPORTANT: we only configure sharding here, if processors are enabled.
		// See the comment in the caller method.
		boolean shardsStatic = SHARDS_STATIC.get( configurationSource );
		List<ShardAssignmentDescriptor> shardAssignmentOrNulls;
		if ( shardsStatic ) {
			int totalShardCount = SHARDS_TOTAL_COUNT.getAndMapOrThrow(
					configurationSource,
					this::checkTotalShardCount,
					log::missingPropertyForStaticSharding
			);
			shardAssignmentOrNulls = SHARDS_ASSIGNED.getAndMapOrThrow(
					configurationSource,
					shardIndices -> toStaticShardAssignments( configurationSource, totalShardCount, shardIndices ),
					log::missingPropertyForStaticSharding
			);
		}
		else {
			shardAssignmentOrNulls = Collections.singletonList( null );
		}

		OutboxPollingEventProcessor.Factory factory = OutboxPollingEventProcessor.factory( context.mapping(), configurationSource );

		scheduledExecutor = context.threadPoolProvider()
				.newScheduledExecutor( shardAssignmentOrNulls.size(), OutboxPollingEventProcessor.NAME_PREFIX );
		eventProcessors = new ArrayList<>();
		for ( ShardAssignmentDescriptor shardAssignmentOrNull : shardAssignmentOrNulls ) {
			eventProcessors.add( factory.create( scheduledExecutor, finderProviderHolder.get(),
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
						SHARDS_TOTAL_COUNT.resolveOrRaw( configurationPropertySource ) );
			}
		}
		List<ShardAssignmentDescriptor> shardAssignment = new ArrayList<>();
		for ( Integer shardIndex : uniqueShardIndices ) {
			shardAssignment.add( new ShardAssignmentDescriptor( totalShardCount, shardIndex ) );
		}
		return shardAssignment;
	}

	@Override
	public CompletableFuture<?> completion() {
		if ( eventProcessors == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[eventProcessors.size()];
		int i = 0;
		for ( OutboxPollingEventProcessor eventProcessor : eventProcessors ) {
			futures[i] = eventProcessor.completion();
			i++;
		}
		return CompletableFuture.allOf( futures );
	}

	@Override
	public CompletableFuture<?> preStop(CoordinationStrategyPreStopContext context) {
		if ( eventProcessors == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[eventProcessors.size()];
		int i = 0;
		for ( OutboxPollingEventProcessor eventProcessor : eventProcessors ) {
			futures[i] = eventProcessor.preStop();
			i++;
		}
		return CompletableFuture.allOf( futures );
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( OutboxPollingEventProcessor::stop, eventProcessors );
			closer.push( ScheduledExecutorService::shutdownNow, scheduledExecutor );
			closer.push( BeanHolder::close, finderProviderHolder );
			closer.push( BeanHolder::close, agentRepositoryProviderHolder );
		}
	}
}
