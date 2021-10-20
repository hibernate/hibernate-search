/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import org.hibernate.search.mapper.orm.coordination.databasepolling.cfg.HibernateOrmMapperDatabasePollingSettings;
import org.hibernate.search.mapper.orm.coordination.databasepolling.cfg.impl.HibernateOrmMapperDatabasePollingImplSettings;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.DatabasePollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.DatabasePollingOutboxEventSendingPlan;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.EntityIdHashRangeOutboxEventPredicate;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.OutboxEventBackgroundProcessor;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.OutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl.OutboxEventPredicate;
import org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl.Log;
import org.hibernate.search.util.common.data.impl.RangeCompatibleHashFunction;
import org.hibernate.search.util.common.data.impl.RangeHashTable;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class DatabasePollingCooordinationStrategy implements CooordinationStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> SHARDS_STATIC =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.SHARDS_STATIC )
					.asBoolean()
					.withDefault( HibernateOrmMapperDatabasePollingSettings.Defaults.COORDINATION_SHARDS_STATIC )
					.build();

	private static final OptionalConfigurationProperty<Integer> SHARDS_TOTAL_COUNT =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.SHARDS_TOTAL_COUNT )
					.asIntegerStrictlyPositive()
					.build();

	private static final OptionalConfigurationProperty<List<Integer>> SHARDS_ASSIGNED =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.SHARDS_ASSIGNED )
					.asIntegerPositiveOrZero()
					.multivalued()
					.build();

	private static final ConfigurationProperty<Boolean> PROCESSORS_INDEXING_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.PROCESSORS_INDEXING_ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperDatabasePollingSettings.Defaults.COORDINATION_PROCESSORS_INDEXING_ENABLED )
					.build();

	private static final ConfigurationProperty<Integer> PROCESSORS_INDEXING_POLLING_INTERVAL =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.PROCESSORS_INDEXING_POLLING_INTERVAL )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperDatabasePollingSettings.Defaults.COORDINATION_PROCESSORS_INDEXING_POLLING_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PROCESSORS_INDEXING_BATCH_SIZE =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.PROCESSORS_INDEXING_BATCH_SIZE )
					.asIntegerStrictlyPositive()
					.withDefault( HibernateOrmMapperDatabasePollingSettings.Defaults.COORDINATION_PROCESSORS_INDEXING_BATCH_SIZE )
					.build();

	private static final OptionalConfigurationProperty<BeanReference<? extends OutboxEventFinderProvider>> PROCESSORS_INDEXING_OUTBOX_EVENT_FINDER_PROVIDER =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingImplSettings.CoordinationRadicals.PROCESSORS_INDEXING_OUTBOX_EVENT_FINDER_PROVIDER )
					.asBeanReference( OutboxEventFinderProvider.class )
					.build();

	private static final OptionalConfigurationProperty<Integer> PROCESSORS_INDEXING_TRANSACTION_TIMEOUT =
			ConfigurationProperty.forKey( HibernateOrmMapperDatabasePollingSettings.CoordinationRadicals.PROCESSORS_INDEXING_TRANSACTION_TIMEOUT )
					.asIntegerStrictlyPositive()
					.build();

	public static final String PROCESSOR_NAME_PREFIX = "Outbox event processor";

	private BeanHolder<? extends OutboxEventFinderProvider> finderProviderHolder;
	private ScheduledExecutorService scheduledExecutor;
	private List<Integer> assignedShardIndices;
	private RangeHashTable<OutboxEventBackgroundProcessor> indexingProcessors;

	@Override
	public void configure(CoordinationConfigurationContext context) {
		context.mappingProducer( new DatabasePollingOutboxEventAdditionalJaxbMappingProducer() );
		context.sendIndexingEventsTo( ctx -> new DatabasePollingOutboxEventSendingPlan( ctx.session() ), true );
	}

	@Override
	public CompletableFuture<?> start(CoordinationStrategyStartContext context) {
		Optional<BeanHolder<? extends OutboxEventFinderProvider>> finderProviderHolderOptional =
				PROCESSORS_INDEXING_OUTBOX_EVENT_FINDER_PROVIDER.getAndMap(
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

		if ( PROCESSORS_INDEXING_ENABLED.get( context.configurationPropertySource() ) ) {
			initializeProcessors( context );
		}
		else {
			// IMPORTANT: in this case we don't even configure sharding, and that's on purpose.
			// Application developers may want a fixed number of processing nodes (static sharding)
			// with a varying number of non-processing nodes,
			// so we want to make those non-processing nodes easy to configure.
			log.indexingProcessorDisabled();
		}

		return CompletableFuture.completedFuture( null );
	}

	private void initializeProcessors(CoordinationStrategyStartContext context) {
		ConfigurationPropertySource configurationSource = context.configurationPropertySource();

		// IMPORTANT: we only configure sharding here, if processors are enabled.
		// See the comment in the caller method.
		boolean shardsStatic = SHARDS_STATIC.get( configurationSource );
		int totalShardCount;
		if ( shardsStatic ) {
			totalShardCount = SHARDS_TOTAL_COUNT.getAndMapOrThrow(
					configurationSource,
					this::checkTotalShardCount,
					log::missingPropertyForStaticSharding
			);
			this.assignedShardIndices = SHARDS_ASSIGNED.getAndMapOrThrow(
					configurationSource,
					shardIndices -> checkAssignedShardIndices(
							configurationSource, totalShardCount, shardIndices ),
					log::missingPropertyForStaticSharding
			);
		}
		else {
			log.warnf( "Dynamic sharding is not implemented yet; defaulting to static sharding assuming a single node" );
			totalShardCount = 1;
			this.assignedShardIndices = Collections.singletonList( 0 );
		}

		int pollingInterval = PROCESSORS_INDEXING_POLLING_INTERVAL.get( configurationSource );
		int batchSize = PROCESSORS_INDEXING_BATCH_SIZE.get( configurationSource );
		Integer transactionTimeout = PROCESSORS_INDEXING_TRANSACTION_TIMEOUT.get( configurationSource )
				.orElse( null );

		scheduledExecutor = context.threadPoolProvider()
				.newScheduledExecutor( this.assignedShardIndices.size(), PROCESSOR_NAME_PREFIX );
		// Note the hash function / table implementations MUST NOT CHANGE,
		// otherwise existing indexes will no longer work correctly.
		RangeCompatibleHashFunction hashFunction = DatabasePollingOutboxEventSendingPlan.HASH_FUNCTION;
		indexingProcessors = new RangeHashTable<>( hashFunction, totalShardCount );
		for ( int shardIndex : this.assignedShardIndices ) {
			Optional<OutboxEventPredicate> predicate = totalShardCount == 1
					? Optional.empty()
					: Optional.of( new EntityIdHashRangeOutboxEventPredicate( indexingProcessors.rangeForBucket( shardIndex ) ) );
			OutboxEventFinder finder = finderProviderHolder.get().create( predicate );
			OutboxEventBackgroundProcessor processor = new OutboxEventBackgroundProcessor(
					PROCESSOR_NAME_PREFIX + " - " + shardIndex,
					context.mapping(), scheduledExecutor, finder, pollingInterval, batchSize, transactionTimeout );
			indexingProcessors.set( shardIndex, processor );
		}
		for ( int processedShardIndex : this.assignedShardIndices ) {
			indexingProcessors.get( processedShardIndex ).start();
		}
	}

	private Integer checkTotalShardCount(Integer totalShardCount) {
		if ( totalShardCount <= 0 ) {
			throw log.invalidTotalShardCount();
		}
		return totalShardCount;
	}

	private List<Integer> checkAssignedShardIndices(ConfigurationPropertySource configurationPropertySource,
			int totalShardCount, List<Integer> shardIndices) {
		for ( Integer shardIndex : shardIndices ) {
			if ( !( 0 <= shardIndex && shardIndex < totalShardCount ) ) {
				throw log.invalidShardIndex( totalShardCount,
						SHARDS_TOTAL_COUNT.resolveOrRaw( configurationPropertySource ) );
			}
		}
		// Remove duplicates
		return new ArrayList<>( new HashSet<>( shardIndices ) );
	}

	@Override
	public CompletableFuture<?> completion() {
		if ( indexingProcessors == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[assignedShardIndices.size()];
		int i = 0;
		for ( int shardIndex : assignedShardIndices ) {
			futures[i] = indexingProcessors.get( shardIndex ).completion();
			i++;
		}
		return CompletableFuture.allOf( futures );
	}

	@Override
	public CompletableFuture<?> preStop(CoordinationStrategyPreStopContext context) {
		if ( indexingProcessors == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[assignedShardIndices.size()];
		int i = 0;
		for ( int shardIndex : assignedShardIndices ) {
			futures[i] = indexingProcessors.get( shardIndex ).preStop();
			i++;
		}
		return CompletableFuture.allOf( futures );
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( OutboxEventBackgroundProcessor::stop, indexingProcessors );
			closer.push( ScheduledExecutorService::shutdownNow, scheduledExecutor );
			closer.push( BeanHolder::close, finderProviderHolder );
		}
	}
}
