//$Id$
package org.hibernate.search.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.backend.configuration.MaskedProperty;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.optimization.IncrementalOptimizerStrategy;
import org.hibernate.search.store.optimization.NoOpOptimizerStrategy;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.PluginLoader;
import org.hibernate.util.StringHelper;

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

	public DirectoryProviders createDirectoryProviders(XClass entity, SearchConfiguration cfg,
													   SearchFactoryImplementor searchFactoryImplementor,
													   ReflectionManager reflectionManager) {
		//get properties
		String directoryProviderName = getDirectoryProviderName( entity, cfg );
		Properties[] indexProps = getDirectoryProperties( cfg, directoryProviderName );

		//set up the directories
		int nbrOfProviders = indexProps.length;
		DirectoryProvider[] providers = new DirectoryProvider[nbrOfProviders];
		for (int index = 0; index < nbrOfProviders; index++) {
			String providerName = nbrOfProviders > 1 ?
					directoryProviderName + "." + index :
					directoryProviderName;
			providers[index] = createDirectoryProvider( providerName, indexProps[index],
					reflectionManager.toClass( entity ), searchFactoryImplementor );
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
			shardingStrategy = PluginLoader.instanceFromName( IndexShardingStrategy.class,
					shardingStrategyName, DirectoryProviderFactory.class, "IndexShardingStrategy" );
		}
		shardingStrategy.initialize(
				new MaskedProperty( indexProps[0], SHARDING_STRATEGY ), providers );
		return new DirectoryProviders( shardingStrategy, providers );
	}

	public void startDirectoryProviders() {
		for (DirectoryProvider provider : providers) {
			provider.start();
		}
	}

	private DirectoryProvider<?> createDirectoryProvider(String directoryProviderName, Properties indexProps,
														 Class entity, SearchFactoryImplementor searchFactoryImplementor) {
		String className = indexProps.getProperty( "directory_provider" );
		DirectoryProvider<?> provider;
		if ( StringHelper.isEmpty( className ) ) {
			provider = new FSDirectoryProvider();
		}
		else {
			provider = PluginLoader.instanceFromName( DirectoryProvider.class, className,
					DirectoryProviderFactory.class, "directory provider" );
		}
		try {
			provider.initialize( directoryProviderName, indexProps, searchFactoryImplementor );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to initialize directory provider: " + directoryProviderName, e );
		}
		int index = providers.indexOf( provider );
		if ( index != -1 ) {
			//share the same Directory provider for the same underlying store
			final DirectoryProvider<?> directoryProvider = providers.get( index );
			searchFactoryImplementor.addClassToDirectoryProvider( entity, directoryProvider );
			return directoryProvider;
		}
		else {
			configureOptimizerStrategy( searchFactoryImplementor, indexProps, provider );
			configureIndexingParameters( searchFactoryImplementor, indexProps, provider );
			providers.add( provider );
			searchFactoryImplementor.addClassToDirectoryProvider( entity, provider );
			if ( !searchFactoryImplementor.getDirectoryProviders().contains( provider ) ) {
				searchFactoryImplementor.addDirectoryProvider( provider );
			}
			return provider;
		}
	}

	private void configureOptimizerStrategy(SearchFactoryImplementor searchFactoryImplementor, Properties indexProps, DirectoryProvider<?> provider) {
		boolean incremental = indexProps.containsKey( "optimizer.operation_limit.max" )
				|| indexProps.containsKey( "optimizer.transaction_limit.max" );
		OptimizerStrategy optimizerStrategy;
		if ( incremental ) {
			optimizerStrategy = new IncrementalOptimizerStrategy();
			optimizerStrategy.initialize( provider, indexProps, searchFactoryImplementor );
		}
		else {
			optimizerStrategy = new NoOpOptimizerStrategy();
		}
		searchFactoryImplementor.addOptimizerStrategy( provider, optimizerStrategy );
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
	 * @param searchFactoryImplementor the search factory.
	 * @param directoryProperties	  The properties extracted from the configuration.
	 * @param provider				 The directory provider for which to configure the indexing parameters.
	 */
	private void configureIndexingParameters(SearchFactoryImplementor searchFactoryImplementor,
											 Properties directoryProperties, DirectoryProvider<?> provider) {
		LuceneIndexingParameters indexingParams = new LuceneIndexingParameters( directoryProperties );
		searchFactoryImplementor.addIndexingParameters( provider, indexingParams );
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
			int shardsCount = ConfigurationParseHelper.parseInt( shardsCountValue, shardsCountValue + " is not a number" );
			// create shard-specific Props
			Properties[] shardLocalProperties = new Properties[shardsCount];
			for (int i = 0; i < shardsCount; i++) {
				shardLocalProperties[i] = new MaskedProperty(
						directoryLocalProperties, Integer.toString( i ), directoryLocalProperties );
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
		Class aClass = cfg.getClassMapping( clazz.getName() );
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
					"Trying to extract the index name from a non @Indexed class: " + clazz.getName() );
		}
	}

	public static class DirectoryProviders {
		private final IndexShardingStrategy shardingStrategy;
		private final DirectoryProvider[] providers;

		public DirectoryProviders(IndexShardingStrategy shardingStrategy, DirectoryProvider[] providers) {
			this.shardingStrategy = shardingStrategy;
			this.providers = providers;
		}

		public IndexShardingStrategy getSelectionStrategy() {
			return shardingStrategy;
		}

		public DirectoryProvider[] getProviders() {
			return providers;
		}
	}

}
