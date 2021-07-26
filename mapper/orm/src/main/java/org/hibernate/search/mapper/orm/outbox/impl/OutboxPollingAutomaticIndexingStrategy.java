/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outbox.impl;

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
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingConfigurationContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyPreStopContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategyStartContext;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.impl.HibernateOrmMapperImplSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.data.impl.RangeCompatibleHashFunction;
import org.hibernate.search.util.common.data.impl.RangeHashTable;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class OutboxPollingAutomaticIndexingStrategy implements AutomaticIndexingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanReference<? extends OutboxEventFinderProvider>> OUTBOX_EVENT_FINDER_PROVIDER =
			ConfigurationProperty.forKey(
					HibernateOrmMapperImplSettings.AutomaticIndexingRadicals.OUTBOX_EVENT_FINDER_PROVIDER )
					.asBeanReference( OutboxEventFinderProvider.class )
					.build();

	private static final ConfigurationProperty<Boolean> PROCESSING_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.PROCESSING_ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_PROCESSING_ENABLED )
					.build();

	private static final ConfigurationProperty<Integer> PROCESSING_POLLING_INTERVAL =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.PROCESSING_POLLING_INTERVAL )
					.asInteger()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_PROCESSING_POLLING_INTERVAL )
					.build();

	private static final ConfigurationProperty<Integer> PROCESSING_BATCH_SIZE =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.PROCESSING_BATCH_SIZE )
					.asInteger()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_PROCESSING_BATCH_SIZE )
					.build();

	private static final ConfigurationProperty<Boolean> PROCESSING_SHARDS_STATIC =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.PROCESSING_SHARDS_STATIC )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_PROCESSING_SHARDS_STATIC )
					.build();

	private static final OptionalConfigurationProperty<Integer> PROCESSING_SHARDS_TOTAL_COUNT =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.PROCESSING_SHARDS_TOTAL_COUNT )
					.asInteger()
					.build();

	private static final OptionalConfigurationProperty<List<Integer>> PROCESSING_SHARDS_ASSIGNED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.AutomaticIndexingRadicals.PROCESSING_SHARDS_ASSIGNED )
					.asInteger()
					.multivalued()
					.build();

	public static final String PROCESSOR_NAME_PREFIX = "Outbox table automatic indexing";

	private BeanHolder<? extends OutboxEventFinderProvider> finderProviderHolder;
	private ScheduledExecutorService scheduledExecutor;
	private List<Integer> assignedShardIndices;
	private RangeHashTable<OutboxEventBackgroundProcessor> processors;

	@Override
	public void configure(AutomaticIndexingConfigurationContext context) {
		context.sendIndexingEventsTo( ctx -> new OutboxEventSendingPlan( ctx.session() ), true );
	}

	@Override
	public CompletableFuture<?> start(AutomaticIndexingStrategyStartContext context) {
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

		if ( PROCESSING_ENABLED.get( context.configurationPropertySource() ) ) {
			initializeProcessors( context );
		}
		else {
			log.outboxEventProcessingDisabled();
		}

		return CompletableFuture.completedFuture( null );
	}

	private void initializeProcessors(AutomaticIndexingStrategyStartContext context) {
		ConfigurationPropertySource configurationSource = context.configurationPropertySource();

		boolean shardsStatic = PROCESSING_SHARDS_STATIC.get( configurationSource );
		int totalShardCount;
		if ( shardsStatic ) {
			totalShardCount = PROCESSING_SHARDS_TOTAL_COUNT.getAndMapOrThrow(
					configurationSource,
					this::checkTotalShardCount,
					log::missingPropertyForStaticSharding
			);
			this.assignedShardIndices = PROCESSING_SHARDS_ASSIGNED.getAndMapOrThrow(
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

		int pollingInterval = PROCESSING_POLLING_INTERVAL.get( context.configurationPropertySource() );
		int batchSize = PROCESSING_BATCH_SIZE.get( context.configurationPropertySource() );

		scheduledExecutor = context.threadPoolProvider()
				.newScheduledExecutor( this.assignedShardIndices.size(), PROCESSOR_NAME_PREFIX );
		// Note the hash function / table implementations MUST NOT CHANGE,
		// otherwise existing indexes will no longer work correctly.
		RangeCompatibleHashFunction hashFunction = OutboxEventSendingPlan.HASH_FUNCTION;
		processors = new RangeHashTable<>( hashFunction, totalShardCount );
		for ( int shardIndex : this.assignedShardIndices ) {
			Optional<OutboxEventPredicate> predicate = totalShardCount == 1
					? Optional.empty()
					: Optional.of( new EntityIdHashRangeOutboxEventPredicate( processors.rangeForBucket( shardIndex ) ) );
			OutboxEventFinder finder = finderProviderHolder.get().create( predicate );
			OutboxEventBackgroundProcessor processor = new OutboxEventBackgroundProcessor(
					PROCESSOR_NAME_PREFIX + " - " + shardIndex,
					context.mapping(), scheduledExecutor, finder, pollingInterval, batchSize );
			processors.set( shardIndex, processor );
		}
		for ( int processedShardIndex : this.assignedShardIndices ) {
			processors.get( processedShardIndex ).start();
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
						PROCESSING_SHARDS_TOTAL_COUNT.resolveOrRaw( configurationPropertySource ) );
			}
		}
		// Remove duplicates
		return new ArrayList<>( new HashSet<>( shardIndices ) );
	}

	@Override
	public CompletableFuture<?> preStop(AutomaticIndexingStrategyPreStopContext context) {
		if ( processors == null ) {
			// Nothing to do
			return CompletableFuture.completedFuture( null );
		}
		CompletableFuture<?>[] futures = new CompletableFuture[assignedShardIndices.size()];
		int i = 0;
		for ( int shardIndex : assignedShardIndices ) {
			futures[i] = processors.get( shardIndex ).preStop();
			i++;
		}
		return CompletableFuture.allOf( futures );
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( OutboxEventBackgroundProcessor::stop, processors );
			closer.push( ScheduledExecutorService::shutdownNow, scheduledExecutor );
			closer.push( BeanHolder::close, finderProviderHolder );
		}
	}
}
