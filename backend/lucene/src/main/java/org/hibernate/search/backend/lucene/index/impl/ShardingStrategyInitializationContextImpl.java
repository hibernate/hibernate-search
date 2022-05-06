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
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.DirectoryCreationContextImpl;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IOStrategy;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class ShardingStrategyInitializationContextImpl implements ShardingStrategyInitializationContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<BeanReference<? extends ShardingStrategy>> SHARDING_STRATEGY =
			ConfigurationProperty.forKey( LuceneIndexSettings.ShardingRadicals.STRATEGY )
					.asBeanReference( ShardingStrategy.class )
					.withDefault( BeanReference.of(
							ShardingStrategy.class, LuceneIndexSettings.Defaults.SHARDING_STRATEGY
					) )
					.build();

	private static final ConfigurationProperty<BeanReference<? extends DirectoryProvider>> DIRECTORY_TYPE =
			ConfigurationProperty.forKey( LuceneIndexSettings.DIRECTORY_TYPE )
					.asBeanReference( DirectoryProvider.class )
					.withDefault( BeanReference.of( DirectoryProvider.class, LuceneIndexSettings.Defaults.DIRECTORY_TYPE ) )
					.build();

	private final IndexManagerBackendContext backendContext;
	private final LuceneIndexModel model;
	private final IndexManagerStartContext startContext;
	private final ConfigurationPropertySource indexPropertySource;
	private final ConfigurationPropertySource shardingPropertySource;

	private Set<String> shardIdentifiers = new LinkedHashSet<>();

	ShardingStrategyInitializationContextImpl(IndexManagerBackendContext backendContext,
			LuceneIndexModel model, IndexManagerStartContext startContext,
			ConfigurationPropertySource indexPropertySource) {
		this.backendContext = backendContext;
		this.model = model;
		this.startContext = startContext;
		this.indexPropertySource = indexPropertySource;
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

	public BeanHolder<? extends ShardingStrategy> create(Map<String, Shard> shardCollector,
			Map<String, SavedState> states) {
		BeanHolder<? extends ShardingStrategy> shardingStrategyHolder =
				SHARDING_STRATEGY.getAndTransform( shardingPropertySource, beanResolver()::resolve );

		shardingStrategyHolder.get().initialize( this );

		if ( shardIdentifiers == null ) {
			// Sharding is disabled => single shard
			contributeShardWithSilentFailure( shardCollector, Optional.empty(),
					states.getOrDefault( null, SavedState.empty() ) );
			return null;
		}

		if ( shardIdentifiers.isEmpty() ) {
			throw log.missingShardIdentifiersAfterShardingStrategyInitialization(
					shardingStrategyHolder.get()
			);
		}

		for ( String shardIdentifier : shardIdentifiers ) {
			contributeShardWithSilentFailure( shardCollector, Optional.of( shardIdentifier ),
					states.getOrDefault( shardIdentifier, SavedState.empty() ) );
		}

		return shardingStrategyHolder;
	}

	private void contributeShardWithSilentFailure(Map<String, Shard> shardCollector, Optional<String> shardId,
			SavedState savedState) {
		EventContext shardEventContext = EventContexts.fromIndexNameAndShardId( indexName(), shardId );
		ConfigurationPropertySource shardPropertySource =
				shardId.isPresent() ?
						indexPropertySource.withMask( LuceneIndexSettings.SHARDS ).withMask( shardId.get() )
								.withFallback( indexPropertySource )
						: indexPropertySource;


		Optional<DirectoryHolder> savedDirectoryHolder = savedState.get( Shard.DIRECTORY_HOLDER_KEY );
		DirectoryHolder directoryHolder = null;
		boolean reuseAlreadyStaredDirectoryHolder = false;

		try {
			if ( savedDirectoryHolder.isPresent() ) {
				directoryHolder = savedDirectoryHolder.get();
				reuseAlreadyStaredDirectoryHolder = true;
			}
			else {
				try ( BeanHolder<? extends DirectoryProvider> directoryProviderHolder =
						DIRECTORY_TYPE.getAndTransform( shardPropertySource, startContext.beanResolver()::resolve ) ) {
					DirectoryCreationContext context = new DirectoryCreationContextImpl( shardEventContext,
							indexName(), shardId, beanResolver(), shardPropertySource.withMask( "directory" ) );
					directoryHolder = directoryProviderHolder.get().createDirectoryHolder( context );
				}
			}

			IOStrategy ioStrategy = backendContext.createIOStrategy( shardPropertySource );
			Shard shard = backendContext.createShard( model, shardEventContext, directoryHolder, ioStrategy,
					shardPropertySource, reuseAlreadyStaredDirectoryHolder );
			shardCollector.put( shardId.orElse( null ), shard );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( directoryHolder );

			ContextualFailureCollector failureCollector = startContext.failureCollector();
			if ( shardId.isPresent() ) {
				failureCollector = failureCollector.withContext( EventContexts.fromShardId( shardId.get() ) );
			}
			failureCollector.add( e );
		}
	}
}
