//$Id$
package org.hibernate.search.store;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.impl.SearchFactoryImpl;
import org.hibernate.search.store.optimization.IncrementalOptimizerStrategy;
import org.hibernate.search.store.optimization.NoOpOptimizerStrategy;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Create a Lucene directory provider
 * <p/>
 * Lucene directory providers are configured through properties
 * <ul>
 * 	<li>hibernate.search.default.* and</li>
 * 	<li>hibernate.search.&lt;indexname&gt;.*</li>
 * </ul>
 * <p/>
 * &lt;indexname&gt; properties have precedence over default
 * <p/>
 * The implementation is described by
 * hibernate.search.[default|indexname].directory_provider
 * <p/>
 * If none is defined the default value is FSDirectory
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 */
public class DirectoryProviderFactory {
	public List<DirectoryProvider<?>> providers = new ArrayList<DirectoryProvider<?>>();
	private static String LUCENE_PREFIX = "hibernate.search.";
	private static String LUCENE_DEFAULT = LUCENE_PREFIX + "default.";
	private static String DEFAULT_DIRECTORY_PROVIDER = FSDirectoryProvider.class.getName();
	
	private static final String SHARDING_STRATEGY = "sharding_strategy";
	private static final String NBR_OF_SHARDS = SHARDING_STRATEGY + ".nbr_of_shards";


	public DirectoryProviders createDirectoryProviders(XClass entity, Configuration cfg, SearchFactoryImplementor searchFactoryImplementor) {
		//get properties
		String directoryProviderName = getDirectoryProviderName( entity, cfg );
		Properties[] indexProps = getDirectoryProperties( cfg, directoryProviderName );

		//set up the directories
		int nbrOfProviders = indexProps.length;
		DirectoryProvider[] providers = new DirectoryProvider[nbrOfProviders];
		for (int index = 0 ; index < nbrOfProviders ; index++) {
			String providerName = nbrOfProviders > 1 ?
					directoryProviderName + "." + index :
					directoryProviderName;
			providers[index] = createDirectoryProvider( providerName,indexProps[index], searchFactoryImplementor);
		}

		//define sharding strategy
		IndexShardingStrategy shardingStrategy;
		Properties shardingProperties = new Properties();
		//we use an enumeration to get the keys from defaultProperties as well
		Enumeration<String> allProps = (Enumeration<String>) indexProps[0].propertyNames();
		while ( allProps.hasMoreElements() ){
			String key = allProps.nextElement();
			if ( key.startsWith( SHARDING_STRATEGY ) ) {
				shardingProperties.put( key, indexProps[0].getProperty( key ) );
			}
		}

		String shardingStrategyName = shardingProperties.getProperty( SHARDING_STRATEGY );
		if ( shardingStrategyName == null) {
			if ( indexProps.length == 1 ) {
				shardingStrategy = new NotShardedStrategy();
			}
			else {
				shardingStrategy = new IdHashShardingStrategy();
			}
		}
		else {
			try {
				Class shardigStrategyClass = ReflectHelper.classForName( shardingStrategyName, this.getClass() );
				shardingStrategy = (IndexShardingStrategy) shardigStrategyClass.newInstance();
			}
			catch (ClassNotFoundException e) {
				throw new SearchException("Unable to find ShardingStrategy class " + shardingStrategyName + " for " + directoryProviderName, e);
			}
			catch (IllegalAccessException e) {
				throw new SearchException("Unable to create instance of ShardingStrategy class " + shardingStrategyName
						+ " Be sure to have a no-arg constructor", e);
			}
			catch (InstantiationException e) {
				throw new SearchException("Unable to create instance of ShardingStrategy class " + shardingStrategyName
						+ " Be sure to have a no-arg constructor", e);
			}
			catch (ClassCastException e) {
				throw new SearchException("ShardingStrategy class does not implements DirecotryProviderShardingStrategy: "
						+ shardingStrategyName, e);
			}
		}
		shardingStrategy.initialize( shardingProperties, providers );

		return new DirectoryProviders( shardingStrategy, providers );
	}

	public void startDirectoryProviders() {
		for ( DirectoryProvider provider : providers ) {
			provider.start();
		}
	}

	private DirectoryProvider<?> createDirectoryProvider(String directoryProviderName, Properties indexProps, SearchFactoryImplementor searchFactoryImplementor) {
		String className = indexProps.getProperty( "directory_provider" );
		if ( StringHelper.isEmpty( className ) ) {
			className = DEFAULT_DIRECTORY_PROVIDER;
		}
		DirectoryProvider<?> provider;
		try {
			@SuppressWarnings( "unchecked" )
			Class<DirectoryProvider> directoryClass = ReflectHelper.classForName(
					className, DirectoryProviderFactory.class
			);
			provider = directoryClass.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to instanciate directory provider: " + className, e );
		}
		try {
			provider.initialize( directoryProviderName, indexProps, searchFactoryImplementor );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to initialize: " + directoryProviderName, e);
		}
		int index = providers.indexOf( provider );
		if ( index != -1 ) {
			//share the same Directory provider for the same underlying store
			return providers.get( index );
		}
		else {
			configureOptimizerStrategy(searchFactoryImplementor, indexProps, provider);
			configureIndexingParameters(searchFactoryImplementor, indexProps, provider);
			providers.add( provider );
			if ( !searchFactoryImplementor.getLockableDirectoryProviders().containsKey( provider ) ) {
				searchFactoryImplementor.getLockableDirectoryProviders().put( provider, new ReentrantLock() );
			}
			return provider;
		}
	}

	private void configureOptimizerStrategy(SearchFactoryImplementor searchFactoryImplementor, Properties indexProps, DirectoryProvider<?> provider) {
		boolean incremental = indexProps.containsKey( "optimizer.operation_limit.max" )
				|| indexProps.containsKey( "optimizer.transaction_limit.max" );
		OptimizerStrategy optimizerStrategy;
		if (incremental) {
			optimizerStrategy = new IncrementalOptimizerStrategy();
			optimizerStrategy.initialize( provider, indexProps, searchFactoryImplementor);
		}
		else {
			optimizerStrategy = new NoOpOptimizerStrategy();
		}
		searchFactoryImplementor.addOptimizerStrategy(provider, optimizerStrategy);
	}
	
	/**
	 * Creates a new <code>LuceneIndexingParameters</code> instance for the specified provider. 
	 * If there are no matching properties in the configuration default values will be applied.
	 * <p>
	 * NOTE:</br>
	 * If a non batch value is set in the configuration apply it also to the
     * batch mode. This covers the case where users only specify 
	 * parameters for the non batch mode. In this case the same parameters apply for 
	 * batch indexing.
	 * </p>
	 * 
	 * @param searchFactoryImplementor the search factory.
	 * @param indexProps The properties extracted from the configuration.
	 * @param provider The directory provider for which to configure the indexing parameters.
	 */
	private void configureIndexingParameters(SearchFactoryImplementor searchFactoryImplementor, Properties indexProps, DirectoryProvider<?> provider) {
		LuceneIndexingParameters indexingParams = new LuceneIndexingParameters( indexProps );
		searchFactoryImplementor.addIndexingParmeters( provider, indexingParams );
	}

	/**
	 * Returns an array of directory properties
	 * Properties are defaulted. For a given property name,
	 * hibernate.search.indexname.n has priority over hibernate.search.indexname which has priority over hibernate.search
	 * If the Index is not sharded, a single Properties is returned
	 * If the index is sharded, the Properties index matches the shard index
	 */	
	private static Properties[] getDirectoryProperties(Configuration cfg, String directoryProviderName) {
		Properties cfgAndImplicitProperties = new Properties();
		// fcg has no defaults, so we may use keySet iteration
		//FIXME not so sure about that cfg.setProperties()?
		for ( Map.Entry entry : cfg.getProperties().entrySet() ) {
			String key = entry.getKey().toString();// casting to String
			if ( key.startsWith( LUCENE_PREFIX ) ) {
				//put regular properties and add an explicit batch property when a transaction property is set
				cfgAndImplicitProperties.put( key, entry.getValue() );
				if ( key.contains( LuceneIndexingParameters.TRANSACTION ) ) {
					//FIXME fix that transaction can appear in the index name
					//I imagine checking the last '.transaction.' is safe.
					String additionalKey = key.replaceFirst(LuceneIndexingParameters.TRANSACTION, LuceneIndexingParameters.BATCH);
					if ( cfg.getProperty(additionalKey) == null ){
						cfgAndImplicitProperties.put(additionalKey, cfg.getProperty(key) );
					}
				}
			}
		}
		Properties globalProperties = new Properties();
		Properties directoryLocalProperties = new Properties( globalProperties );
		String directoryLocalPrefix = LUCENE_PREFIX + directoryProviderName + ".";
		for ( Map.Entry entry : cfgAndImplicitProperties.entrySet() ) {
			String key = entry.getKey().toString();// casting to String
			if ( key.startsWith( LUCENE_DEFAULT ) ) {
				globalProperties.put( key.substring( LUCENE_DEFAULT.length() ), entry.getValue() );
			}
			else if ( key.startsWith( directoryLocalPrefix ) ) {
				directoryLocalProperties.put( key.substring( directoryLocalPrefix.length() ),entry.getValue() );
			}
		}
		final String shardsCountValue = directoryLocalProperties.getProperty(NBR_OF_SHARDS);
		if (shardsCountValue == null) {
			// no shards: finished.
			return new Properties[] { directoryLocalProperties };
		} else {
			// count shards
			int shardsCount = -1;
			{
				try {
					shardsCount = Integer.parseInt( shardsCountValue );
				} catch (NumberFormatException e) {
					if ( cfgAndImplicitProperties.getProperty(directoryLocalPrefix + NBR_OF_SHARDS ) != null)
						throw new SearchException( shardsCountValue + " is not a number", e);
				}
			}
			// create shard-specific Props
			Properties[] shardLocalProperties = new Properties[shardsCount];
			for ( int i = 0; i < shardsCount; i++ ) {
				String currentShardPrefix = i + ".";
				Properties currentProp = new Properties( directoryLocalProperties );
				//Enumerations are ugly but otherwise we can't get the property defaults:
				Enumeration<String> localProps = (Enumeration<String>) directoryLocalProperties.propertyNames();
				while ( localProps.hasMoreElements() ){
					String key = localProps.nextElement();
					if ( key.startsWith( currentShardPrefix ) ) {
						currentProp.setProperty( key.substring( currentShardPrefix.length() ), directoryLocalProperties.getProperty( key ) );
					}
				}
				shardLocalProperties[i] = currentProp;
			}
			return shardLocalProperties;
		}
	}

	private static String getDirectoryProviderName(XClass clazz, Configuration cfg) {
		//yuk
		ReflectionManager reflectionManager = SearchFactoryImpl.getReflectionManager(cfg);
		//get the most specialized (ie subclass > superclass) non default index name
		//if none extract the name from the most generic (superclass > subclass) @Indexed class in the hierarchy
		//FIXME I'm inclined to get rid of the default value
		PersistentClass pc = cfg.getClassMapping( clazz.getName() );
		XClass rootIndex = null;
		do {
			XClass currentClazz = reflectionManager.toXClass( pc.getMappedClass() );
			Indexed indexAnn = currentClazz.getAnnotation( Indexed.class );
			if ( indexAnn != null ) {
				if ( indexAnn.index().length() != 0 ) {
					return indexAnn.index();
				}
				else {
					rootIndex = currentClazz;
				}
			}
			pc = pc.getSuperclass();
		}
		while ( pc != null );
		//there is nobody out there with a non default @Indexed.index
		if ( rootIndex != null ) {
			return rootIndex.getName();
		}
		else {
			throw new HibernateException(
					"Trying to extract the index name from a non @Indexed class: " + clazz.getName() );
		}
	}

	public class DirectoryProviders {
		private IndexShardingStrategy shardingStrategy;
		private DirectoryProvider[] providers;


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
