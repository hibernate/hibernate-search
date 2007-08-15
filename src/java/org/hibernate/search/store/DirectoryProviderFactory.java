//$Id$
package org.hibernate.search.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.impl.SearchFactoryImpl;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.store.optimization.IncrementalOptimizerStrategy;
import org.hibernate.search.store.optimization.NoOpOptimizerStrategy;
import org.hibernate.search.SearchException;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
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
	
	// Lucene index performance parameters
	private static final String MERGE_FACTOR = "merge_factor";
	private static final String MAX_MERGE_DOCS = "max_merge_docs";
	private static final String MAX_BUFFERED_DOCS = "max_buffered_docs";
	private static final String BATCH = "batch.";
	private static final String TRANSACTION = "transaction.";

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
		for (Map.Entry entry : indexProps[0].entrySet()) {
			if ( ( (String) entry.getKey() ).startsWith( SHARDING_STRATEGY ) ) {
				shardingProperties.put( entry.getKey(), entry.getValue() );
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
	 * If a non  batch value is set in the configuration apply it also to the
     * batch mode. This covers the case where users only specify 
	 * paramters for the non batch mode. In this case the same parameters apply for 
	 * batch indexing.
	 * </p>
	 * 
	 * @param searchFactoryImplementor the search factory.
	 * @param indexProps The properties extracted from the configuration.
	 * @param provider The direcotry provider for which to configure the indexing parameters.
	 */
	private void configureIndexingParameters(SearchFactoryImplementor searchFactoryImplementor, Properties indexProps, DirectoryProvider<?> provider) {
		
		LuceneIndexingParameters indexingParams = new LuceneIndexingParameters();
		String s = indexProps.getProperty(TRANSACTION + MERGE_FACTOR);
		
		if (!StringHelper.isEmpty( s )) {
			try{
				indexingParams.setTransactionMergeFactor(Integer.valueOf(s));
				indexingParams.setBatchMergeFactor(Integer.valueOf(s));
			} catch (NumberFormatException ne) {
				throw new SearchException("Invalid value for " + TRANSACTION + MERGE_FACTOR + ": " + s);
			}
		}

		s = indexProps.getProperty(TRANSACTION + MAX_MERGE_DOCS);
		if (!StringHelper.isEmpty( s )) {
			try{
				indexingParams.setTransactionMaxMergeDocs(Integer.valueOf(s));
				indexingParams.setBatchMaxMergeDocs(Integer.valueOf(s));
			} catch (NumberFormatException ne) {
				throw new SearchException("Invalid value for " + TRANSACTION + MAX_MERGE_DOCS + ": " + s);
			}
		}
		
		s = indexProps.getProperty(TRANSACTION + MAX_BUFFERED_DOCS);
		if (!StringHelper.isEmpty( s )) {
			try{
				indexingParams.setTransactionMaxBufferedDocs(Integer.valueOf(s));
				indexingParams.setBatchMaxBufferedDocs(Integer.valueOf(s));
			} catch (NumberFormatException ne) {
				throw new SearchException("Invalid value for " + TRANSACTION + MAX_BUFFERED_DOCS + ": " + s);
			}
		}		
				
		s = indexProps.getProperty(BATCH + MERGE_FACTOR);
		if (!StringHelper.isEmpty( s )) {
			try{
				indexingParams.setBatchMergeFactor(Integer.valueOf(s));
			} catch (NumberFormatException ne) {
				throw new SearchException("Invalid value for " + BATCH + MERGE_FACTOR + ": " + s);
			}
		}
		
		s = indexProps.getProperty(BATCH + MAX_MERGE_DOCS);
		if (!StringHelper.isEmpty( s )) {
			try{
				indexingParams.setBatchMaxMergeDocs(Integer.valueOf(s));
			} catch (NumberFormatException ne) {
				throw new SearchException("Invalid value for " + BATCH + MAX_MERGE_DOCS + ": " + s);
			}
		}
		
		s = indexProps.getProperty(BATCH + MAX_BUFFERED_DOCS);
		if (!StringHelper.isEmpty( s )) {
			try{
				indexingParams.setBatchMaxBufferedDocs(Integer.valueOf(s));
			} catch (NumberFormatException ne) {
				throw new SearchException("Invalid value for " + BATCH + MAX_BUFFERED_DOCS + ": " + s);
			}
		}	
		searchFactoryImplementor.addIndexingParmeters(provider, indexingParams);
	}

	/**
	 * Returns an array of directory properties
	 * Properties are defaulted. For a given proeprty name,
	 * hibernate.search.indexname.n has priority over hibernate.search.indexname which has priority over hibernate.search
	 * If the Index is not sharded, a single Properties is returned
	 * If the index is sharded, the Properties index matches the shard index
	 */
	private static Properties[] getDirectoryProperties(Configuration cfg, String directoryProviderName) {
		Properties props = cfg.getProperties();
		String indexName = LUCENE_PREFIX + directoryProviderName;
		//indexSpecificProperties[i] >> indexSpecificDefaultproperties >> defaultProperties
		Properties defaultProperties = new Properties();
		ArrayList<Properties> indexSpecificProps = new ArrayList<Properties>();
		Properties indexSpecificDefaultProps = new Properties(defaultProperties);
		for ( Map.Entry entry : props.entrySet() ) {
			String key = (String) entry.getKey();
			if ( key.startsWith( LUCENE_DEFAULT ) ) {
				defaultProperties.setProperty( key.substring( LUCENE_DEFAULT.length() ), (String) entry.getValue() );
			}
			else if ( key.startsWith( indexName ) ) {
				String suffixedKey = key.substring( indexName.length() + 1 );
				int nextDoc = suffixedKey.indexOf( '.' );
				int index = -1;
				if ( nextDoc != -1 ) {
				    String potentialNbr = suffixedKey.substring( 0, nextDoc );
					try {
						index = Integer.parseInt( potentialNbr );
					}
					catch ( Exception e ) {
						//just not a number
						index = -1;
					}
				}
				if (index == -1) {
					indexSpecificDefaultProps.setProperty( suffixedKey, (String) entry.getValue() );
				}
				else {
					String finalKeyName = suffixedKey.substring( nextDoc + 1 );
					//ignore sharding strategy properties
					if ( ! finalKeyName.startsWith( SHARDING_STRATEGY ) ) {
						ensureListSize( indexSpecificProps, index + 1 );
						Properties propertiesforIndex = indexSpecificProps.get( index );
						if ( propertiesforIndex == null ) {
							propertiesforIndex = new Properties( indexSpecificDefaultProps );
							indexSpecificProps.set( index, propertiesforIndex );
						}
						propertiesforIndex.setProperty( finalKeyName, (String) entry.getValue() );
					}
				}
			}
		}
		String nbrOfShardsString = indexSpecificDefaultProps.getProperty( NBR_OF_SHARDS );
		int nbrOfShards = -1;
		if ( nbrOfShardsString != null ) {
			try {
				nbrOfShards = Integer.parseInt( nbrOfShardsString );
			}
			catch (NumberFormatException e) {
				throw new SearchException(indexName + "." + NBR_OF_SHARDS + " is not a number", e);
			}
		}
		if ( nbrOfShards <= 0 && indexSpecificProps.size() == 0 ) {
			//no shard (a shareded subindex has to have at least one property
			return new Properties[] { indexSpecificDefaultProps };
		}
		else {
			//sharded
			nbrOfShards = nbrOfShards >= indexSpecificDefaultProps.size() ?
					nbrOfShards :
					indexSpecificDefaultProps.size();
			ensureListSize( indexSpecificProps, nbrOfShards );
			for ( int index = 0 ; index < nbrOfShards ; index++ ) {
				if ( indexSpecificProps.get( index ) == null ) {
					indexSpecificProps.set( index, new Properties( indexSpecificDefaultProps ) );
				}
			}
			return indexSpecificProps.toArray( new Properties[ indexSpecificProps.size() ] );
		}
	}

	private static void ensureListSize(ArrayList<Properties> indexSpecificProps, int size) {
		//ensure the index exists
		indexSpecificProps.ensureCapacity( size );
		while ( indexSpecificProps.size() < size ) {
			indexSpecificProps.add(null);
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
