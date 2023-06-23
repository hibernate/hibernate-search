/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategy;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategyInitializationContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ShardingStrategyInitializationContextImpl implements ShardingStrategyInitializationContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<BeanReference<? extends ShardingStrategy>> SHARDING_STRATEGY =
			ConfigurationProperty.forKey( LuceneIndexSettings.ShardingRadicals.STRATEGY )
					.asBeanReference( ShardingStrategy.class )
					.withDefault( BeanReference.of(
							ShardingStrategy.class, LuceneIndexSettings.Defaults.SHARDING_STRATEGY
					) )
					.build();

	private final IndexManagerBackendContext backendContext;
	private final LuceneIndexModel model;
	private final IndexManagerStartContext startContext;
	private final ConfigurationPropertySource shardingPropertySource;

	private Set<String> shardIdentifiers = new LinkedHashSet<>();

	ShardingStrategyInitializationContextImpl(IndexManagerBackendContext backendContext,
			LuceneIndexModel model, IndexManagerStartContext startContext,
			ConfigurationPropertySource indexPropertySource) {
		this.backendContext = backendContext;
		this.model = model;
		this.startContext = startContext;
		this.shardingPropertySource = indexPropertySource.withMask( "sharding" );
	}

	@Override
	public void shardIdentifiers(Set<String> shardIdentifiers) {
		this.shardIdentifiers.clear();
		this.shardIdentifiers.addAll( shardIdentifiers );
	}

	@Override
	public void disableSharding() {
		this.shardIdentifiers = null;
	}

	@Override
	public String indexName() {
		return model.hibernateSearchName();
	}

	@Override
	public BeanResolver beanResolver() {
		return startContext.beanResolver();
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return shardingPropertySource;
	}

	public BeanHolder<? extends ShardingStrategy> create(Map<String, Shard> shardCollector) {
		BeanHolder<? extends ShardingStrategy> shardingStrategyHolder =
				SHARDING_STRATEGY.getAndTransform( shardingPropertySource, beanResolver()::resolve );

		shardingStrategyHolder.get().initialize( this );

		if ( shardIdentifiers == null ) {
			// Sharding is disabled => single shard
			contributeShard( shardCollector, Optional.empty() );
			return null;
		}

		if ( shardIdentifiers.isEmpty() ) {
			throw log.missingShardIdentifiersAfterShardingStrategyInitialization(
					shardingStrategyHolder.get()
			);
		}

		for ( String shardIdentifier : shardIdentifiers ) {
			contributeShard( shardCollector, Optional.of( shardIdentifier ) );
		}

		return shardingStrategyHolder;
	}

	private void contributeShard(Map<String, Shard> shardCollector, Optional<String> shardId) {
		Shard shard = new Shard( shardId, backendContext, model );
		shardCollector.put( shardId.orElse( null ), shard );
	}
}
