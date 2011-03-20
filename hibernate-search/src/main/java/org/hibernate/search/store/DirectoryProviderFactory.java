/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.Similarity;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.backend.configuration.MaskedProperty;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.spi.WritableBuildContext;
import org.hibernate.search.store.optimization.IncrementalOptimizerStrategy;
import org.hibernate.search.store.optimization.NoOpOptimizerStrategy;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.ClassLoaderHelper;

/**
 * Create a Lucene directory provider which can be configured
 * through the following properties:
 * <ul>
 * <li><i>hibernate.search.default.*</i></li>
 * <li><i>hibernate.search.&lt;indexname&gt;.*</i>,</li>
 * </ul>where <i>&lt;indexname&gt;</i> properties have precedence over default ones.
 * <p/>
 * The implementation is described by
 * <i>hibernate.search.[default|indexname].directory_provider</i>.
 * If none is defined the default value is FSDirectory.
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class DirectoryProviderFactory {

	private final List<DirectoryProvider<?>> providers = new ArrayList<DirectoryProvider<?>>();

	private static final String SHARDING_STRATEGY = "sharding_strategy";
	private static final String NBR_OF_SHARDS = SHARDING_STRATEGY + ".nbr_of_shards";

	private static final Map<String, String> defaultProviderClasses;

	static {
		defaultProviderClasses = new HashMap<String, String>( 6 );
		defaultProviderClasses.put( "", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-master", FSMasterDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-slave", FSSlaveDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "ram", RAMDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "infinispan", "org.hibernate.search.infinispan.InfinispanDirectoryProvider" );
	}

	public DirectoryProviders createDirectoryProviders(XClass entity, SearchConfiguration cfg,
													   WritableBuildContext context,
													   ReflectionManager reflectionManager) {
		//get properties
		String directoryProviderName = getDirectoryProviderName( entity, cfg );
		Properties[] indexProps = getDirectoryProperties( cfg, directoryProviderName );

		//set up the directories
		int nbrOfProviders = indexProps.length;
		DirectoryProvider[] providers = new DirectoryProvider[nbrOfProviders];
		for ( int index = 0; index < nbrOfProviders; index++ ) {
			String providerName = nbrOfProviders > 1 ?
					directoryProviderName + "." + index :
					directoryProviderName;
			providers[index] = createDirectoryProvider(
					providerName, indexProps[index],
					reflectionManager.toClass( entity ),
					context
			);
		}

		//define sharding strategy
		IndexShardingStrategy shardingStrategy;
		//any indexProperty will do, the indexProps[0] surely exists.
		String shardingStrategyName = indexProps[0].getProperty( SHARDING_STRATEGY );
		if ( shardingStrategyName == null ) {
			if ( indexProps.length == 1 ) {
				shardingStrategy = new NotShardedStrategy();
			}
			else {
				shardingStrategy = new IdHashShardingStrategy();
			}
		}
		else {
			shardingStrategy = ClassLoaderHelper.instanceFromName(
					IndexShardingStrategy.class,
					shardingStrategyName, DirectoryProviderFactory.class, "IndexShardingStrategy"
			);
		}
		shardingStrategy.initialize(
				new MaskedProperty( indexProps[0], SHARDING_STRATEGY ), providers
		);
		final String similarityClassName = indexProps[0].getProperty( Environment.SIMILARITY_CLASS_PER_INDEX );
		Similarity similarityInstance = null;
		if ( similarityClassName != null ) {
			similarityInstance = ClassLoaderHelper.instanceFromName(
					Similarity.class,
					similarityClassName,
					DirectoryProviderFactory.class,
					"Similarity class for index " + directoryProviderName
			);
		}
		return new DirectoryProviders( shardingStrategy, providers, similarityInstance );
	}

	public void startDirectoryProviders() {
		for ( DirectoryProvider provider : providers ) {
			provider.start();
		}
	}

	private DirectoryProvider<?> createDirectoryProvider(String directoryProviderName, Properties indexProps,
														 Class<?> entity, WritableBuildContext context) {
		String className = indexProps.getProperty( "directory_provider", "" );
		String maybeShortCut = className.toLowerCase();

		DirectoryProvider<?> provider;
		//try and use the built-in shortcuts before loading the provider as a fully qualified class name 
		if ( defaultProviderClasses.containsKey( maybeShortCut ) ) {
			String fullClassName = defaultProviderClasses.get( maybeShortCut );
			provider = ClassLoaderHelper.instanceFromName( DirectoryProvider.class,
					fullClassName, DirectoryProviderFactory.class, "directory provider" );
		}
		else {
			provider = ClassLoaderHelper.instanceFromName(
					DirectoryProvider.class, className,
					DirectoryProviderFactory.class, "directory provider"
			);
		}
		try {
			provider.initialize( directoryProviderName, indexProps, context );
		}
		catch ( Exception e ) {
			throw new SearchException( "Unable to initialize directory provider: " + directoryProviderName, e );
		}
		int index = providers.indexOf( provider );
		boolean exclusiveIndexUsage = isExclusiveIndexUsageEnabled( directoryProviderName, indexProps );
		int maximumQueueSize = extractMaxQueueSize( directoryProviderName, indexProps );
		if ( index != -1 ) {
			//share the same Directory provider for the same underlying store
			final DirectoryProvider<?> directoryProvider = providers.get( index );
			context.addClassToDirectoryProvider( entity, directoryProvider, exclusiveIndexUsage, maximumQueueSize );
			return directoryProvider;
		}
		else {
			configureOptimizerStrategy( context, indexProps, provider );
			configureIndexingParameters( context, indexProps, provider );
			providers.add( provider );
			context.addClassToDirectoryProvider( entity, provider, exclusiveIndexUsage, maximumQueueSize );
			return provider;
		}
	}

	private void configureOptimizerStrategy(WritableBuildContext context, Properties indexProps, DirectoryProvider<?> provider) {
		boolean incremental = indexProps.containsKey( "optimizer.operation_limit.max" )
				|| indexProps.containsKey( "optimizer.transaction_limit.max" );
		OptimizerStrategy optimizerStrategy;
		if ( incremental ) {
			optimizerStrategy = new IncrementalOptimizerStrategy();
			optimizerStrategy.initialize( provider, indexProps, context );
		}
		else {
			optimizerStrategy = new NoOpOptimizerStrategy();
		}
		context.addOptimizerStrategy( provider, optimizerStrategy );
	}

	/**
	 * Creates a new <code>LuceneIndexingParameters</code> instance for the specified provider.
	 * If there are no matching properties in the configuration default values will be applied.
	 * <p>
	 * NOTE:</br>
	 * If a non batch value is set in the configuration apply it also to the
	 * batch mode. This covers the case where users only specify
	 * parameters for the non batch mode. In this case the same parameters apply for
	 * batch indexing. Parameters are found "depth-first": if a batch parameter is set
	 * in a global scope it will take priority on local transaction parameters.
	 * </p>
	 *
	 * @param context the build context.
	 * @param directoryProperties The properties extracted from the configuration.
	 * @param provider The directory provider for which to configure the indexing parameters.
	 */
	private void configureIndexingParameters(WritableBuildContext context,
											 Properties directoryProperties, DirectoryProvider<?> provider) {
		LuceneIndexingParameters indexingParams = new LuceneIndexingParameters( directoryProperties );
		context.addIndexingParameters( provider, indexingParams );
	}

	/**
	 * Returns an array of directory properties
	 * Properties are defaulted. For a given property name,
	 * hibernate.search.indexname.n has priority over hibernate.search.indexname which has priority over hibernate.search.default
	 * If the Index is not sharded, a single Properties is returned
	 * If the index is sharded, the Properties index matches the shard index
	 */
	private static Properties[] getDirectoryProperties(SearchConfiguration cfg, String directoryProviderName) {
		Properties rootCfg = new MaskedProperty( cfg.getProperties(), "hibernate.search" );
		Properties globalProperties = new MaskedProperty( rootCfg, "default" );
		Properties directoryLocalProperties = new MaskedProperty( rootCfg, directoryProviderName, globalProperties );
		final String shardsCountValue = directoryLocalProperties.getProperty( NBR_OF_SHARDS );
		if ( shardsCountValue == null ) {
			// no shards: finished.
			return new Properties[] { directoryLocalProperties };
		}
		else {
			// count shards
			int shardsCount = ConfigurationParseHelper.parseInt(
					shardsCountValue, shardsCountValue + " is not a number"
			);
			// create shard-specific Props
			Properties[] shardLocalProperties = new Properties[shardsCount];
			for ( int i = 0; i < shardsCount; i++ ) {
				shardLocalProperties[i] = new MaskedProperty(
						directoryLocalProperties, Integer.toString( i ), directoryLocalProperties
				);
			}
			return shardLocalProperties;
		}
	}

	private static String getDirectoryProviderName(XClass clazz, SearchConfiguration cfg) {
		ReflectionManager reflectionManager = cfg.getReflectionManager();
		if ( reflectionManager == null ) {
			reflectionManager = new JavaReflectionManager();
		}
		//get the most specialized (ie subclass > superclass) non default index name
		//if none extract the name from the most generic (superclass > subclass) @Indexed class in the hierarchy
		//FIXME I'm inclined to get rid of the default value
		Class<?> aClass = cfg.getClassMapping( clazz.getName() );
		XClass rootIndex = null;
		do {
			XClass currentClazz = reflectionManager.toXClass( aClass );
			Indexed indexAnn = currentClazz.getAnnotation( Indexed.class );
			if ( indexAnn != null ) {
				if ( indexAnn.index().length() != 0 ) {
					return indexAnn.index();
				}
				else {
					rootIndex = currentClazz;
				}
			}
			aClass = aClass.getSuperclass();
		}
		while ( aClass != null );
		//there is nobody out there with a non default @Indexed.index
		if ( rootIndex != null ) {
			return rootIndex.getName();
		}
		else {
			throw new SearchException(
					"Trying to extract the index name from a non @Indexed class: " + clazz.getName()
			);
		}
	}

	public static class DirectoryProviders {
		private final IndexShardingStrategy shardingStrategy;
		private final DirectoryProvider[] providers;
		private final Similarity similarity;

		public DirectoryProviders(IndexShardingStrategy shardingStrategy, DirectoryProvider[] providers, Similarity similarity) {
			this.shardingStrategy = shardingStrategy;
			this.providers = providers;
			this.similarity = similarity;
		}

		public IndexShardingStrategy getSelectionStrategy() {
			return shardingStrategy;
		}

		public DirectoryProvider[] getProviders() {
			return providers;
		}

		public Similarity getSimilarity() {
			return similarity;
		}
	}

	private static boolean isExclusiveIndexUsageEnabled(String directoryProviderName, Properties indexProps) {
		String exclusiveIndexUsageProperty = indexProps.getProperty( Environment.EXCLUSIVE_INDEX_USE, "false" );
		boolean exclusiveIndexUsage = ConfigurationParseHelper.parseBoolean(
				exclusiveIndexUsageProperty,
				"Illegal value for property " + Environment.EXCLUSIVE_INDEX_USE + " on index " + directoryProviderName
		);
		return exclusiveIndexUsage;
	}

	/**
	 * @param directoryProviderName
	 * @param indexProps MaskedProperties for this DirectoryProvider
	 * @return the maximum queue length to be used on this DP
	 */
	private static int extractMaxQueueSize(String directoryProviderName, Properties indexProps) {
		String maxQueueSize = indexProps.getProperty( Environment.MAX_QUEUE_LENGTH );
		if ( maxQueueSize != null ) {
			int parsedInt= ConfigurationParseHelper
					.parseInt( maxQueueSize, Executors.QUEUE_MAX_LENGTH,
					"Illegal value for property " + Environment.MAX_QUEUE_LENGTH + " on index " + directoryProviderName );
			if ( parsedInt < 1 ) {
				throw new SearchException( "Property " + Environment.MAX_QUEUE_LENGTH + " on index "
						+ directoryProviderName + "must be strictly positive" );
			}
			return parsedInt;
		}
		else {
			return Executors.QUEUE_MAX_LENGTH;
		}
	}

}
