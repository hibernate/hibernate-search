/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.impl.DynamicShardingEntityIndexBinding;
import org.hibernate.search.engine.impl.EntityIndexBindingFactory;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.impl.ExtendedSearchIntegratorWithShareableState;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.store.impl.NotShardedStrategy;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stores references to {@code IndexManager} instances, and starts or stops them.
 *
 * Starting {@code IndexManager}s happens by creating new {@code EntityIndexBinding} instances. {@code IndexManager}s are
 * started successively as they are needed (for example based on the sharding strategy).
 *
 * Stopping {@code IndexManager}s can currently only happen all at once.
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
@SuppressWarnings("deprecation")
public class IndexManagerHolder {
	private static final Log log = LoggerFactory.make();
	private static final String SHARDING_STRATEGY = "sharding_strategy";
	private static final String NBR_OF_SHARDS = SHARDING_STRATEGY + ".nbr_of_shards";
	private static final String INDEX_SHARD_ID_SEPARATOR = ".";

	private static final Similarity DEFAULT_SIMILARITY = new DefaultSimilarity();

	private final Map<String, IndexManager> indexManagersRegistry = new ConcurrentHashMap<String, IndexManager>();

	// I currently think it's easier to not hide sharding implementations in a custom
	// IndexManager to make it easier to explicitly a)detect duplicates b)start-stop
	// additional Managers as needed from a dynamic sharding implementation, without having
	// to embed the sharding logic in a manager itself.
	// so now we have a real 1:1 relation between Managers and indexes, and the signature for
	// #getReader() will always return a single "naive" IndexReader.
	// So we get better caching too, as the changed indexes change cache keys on a fine-grained basis
	// (for both fieldCaches and cached filters)
	public synchronized MutableEntityIndexBinding buildEntityIndexBinding(
			XClass entity,
			Class mappedClass,
			SearchConfiguration cfg,
			WorkerBuildContext buildContext
	) {
		String indexName = getIndexName( entity, cfg );
		Properties[] indexProperties = getIndexProperties( cfg, indexName );
		Similarity similarity = createSimilarity( indexName, cfg, indexProperties[0], entity, buildContext );
		boolean isDynamicSharding = isShardingDynamic( indexProperties[0], buildContext );

		IndexManager[] indexManagers = new IndexManager[0];
		if ( !isDynamicSharding ) {
			indexManagers = createIndexManagers(
					indexName,
					indexProperties,
					similarity,
					mappedClass,
					buildContext
			);
		}

		IndexShardingStrategy shardingStrategy = null;
		if ( !isDynamicSharding ) {
			shardingStrategy = createIndexShardingStrategy( indexProperties, indexManagers, buildContext );
		}

		ShardIdentifierProvider shardIdentifierProvider = null;
		if ( isDynamicSharding ) {
			shardIdentifierProvider = createShardIdentifierProvider(
					buildContext, indexProperties[0]
			);
		}

		EntityIndexingInterceptor interceptor = createEntityIndexingInterceptor( entity );

		return EntityIndexBindingFactory.buildEntityIndexBinding(
				entity.getClass(),
				indexManagers,
				shardingStrategy,
				shardIdentifierProvider,
				similarity,
				interceptor,
				isDynamicSharding,
				indexProperties[0],
				indexName,
				buildContext,
				this
		);
	}

	public IndexManager getOrCreateIndexManager(String indexBaseName,
			DynamicShardingEntityIndexBinding entityIndexBinding) {
		return this.getOrCreateIndexManager( indexBaseName, null, entityIndexBinding );
	}

	public IndexManager getOrCreateIndexManager(String indexBaseName,
			String shardName,
			DynamicShardingEntityIndexBinding entityIndexBinding) {
		String indexName = indexBaseName;
		if ( shardName != null ) {
			indexName += INDEX_SHARD_ID_SEPARATOR + shardName;
		}

		IndexManager indexManager = indexManagersRegistry.get( indexName );
		if ( indexManager != null ) {
			indexManager.addContainedEntity( entityIndexBinding.getDocumentBuilder().getBeanClass() );
			return indexManager;
		}
		ExtendedSearchIntegrator searchFactory = entityIndexBinding.getSearchintegrator();
		WorkerBuildContext context;
		//known implementations of SearchFactory passed are MutableSearchFactory and ImmutableSearchFactory
		if ( WorkerBuildContext.class.isAssignableFrom( searchFactory.getClass() ) ) {
			context = (WorkerBuildContext) searchFactory;
		}
		else {
			throw log.assertionFailureCannotCastToWorkerBuilderContext( searchFactory.getClass() );
		}

		Properties properties = entityIndexBinding.getProperties();
		if ( shardName != null ) {
			properties = new MaskedProperty( properties, shardName, properties );
		}

		indexManager = createIndexManager(
				indexName,
				entityIndexBinding.getDocumentBuilder().getBeanClass(),
				entityIndexBinding.getSimilarity(),
				properties,
				context
		);
		indexManager.setSearchFactory( searchFactory );
		return indexManager;
	}

	/**
	 * @return all IndexManager instances
	 */
	public Collection<IndexManager> getIndexManagers() {
		return indexManagersRegistry.values();
	}

	/**
	 * Useful for MutableSearchFactory, this haves all managed IndexManagers
	 * switch over to the new SearchIntegrator.
	 *
	 * @param factory the new SearchIntegrator to set on each IndexManager.
	 */
	public void setActiveSearchIntegrator(ExtendedSearchIntegratorWithShareableState factory) {
		for ( IndexManager indexManager : getIndexManagers() ) {
			indexManager.setSearchFactory( factory );
		}
	}

	/**
	 * Stops all IndexManager instances
	 */
	public synchronized void stop() {
		for ( IndexManager indexManager : getIndexManagers() ) {
			indexManager.destroy();
		}
		indexManagersRegistry.clear();
	}

	/**
	 * @param targetIndexName the name of the IndexManager to look up
	 *
	 * @return the IndexManager, or null if it doesn't exist
	 */
	public IndexManager getIndexManager(String targetIndexName) {
		if ( targetIndexName == null ) {
			throw log.nullIsInvalidIndexName();
		}
		return indexManagersRegistry.get( targetIndexName );
	}

	private Class<? extends EntityIndexingInterceptor> getInterceptorClassFromHierarchy(XClass entity, Indexed indexedAnnotation) {
		Class<? extends EntityIndexingInterceptor> result = indexedAnnotation.interceptor();
		XClass superEntity = entity;
		while ( result == EntityIndexingInterceptor.class ) {
			superEntity = superEntity.getSuperclass();
			//Object.class
			if ( superEntity == null ) {
				return result;
			}
			Indexed indexAnnForSuperclass = superEntity.getAnnotation( Indexed.class );
			result = indexAnnForSuperclass != null ?
					indexAnnForSuperclass.interceptor() :
					result;
		}
		return result;
	}

	private IndexManager createIndexManager(String indexName,
			Similarity indexSimilarity,
			Properties properties,
			WorkerBuildContext workerBuildContext) {
		// get hold of the index manager factory via the service manager
		ServiceManager serviceManager = workerBuildContext.getServiceManager();
		IndexManagerFactory indexManagerFactory = serviceManager.requestService( IndexManagerFactory.class );

		// create IndexManager instance via the index manager factory
		String indexManagerImplementationName = properties.getProperty( Environment.INDEX_MANAGER_IMPL_NAME );
		final IndexManager manager;
		try {
			if ( StringHelper.isEmpty( indexManagerImplementationName ) ) {
				manager = indexManagerFactory.createDefaultIndexManager();
			}
			else {
				manager = indexManagerFactory.createIndexManagerByName( indexManagerImplementationName );
			}
		}
		finally {
			serviceManager.releaseService( IndexManagerFactory.class );
		}

		// init the IndexManager
		try {
			manager.initialize( indexName, properties, indexSimilarity, workerBuildContext );
			return manager;
		}
		catch (Exception e) {
			throw log.unableToInitializeIndexManager( indexName, e );
		}
	}


	/**
	 * Extracts the index name used for the entity.
	 *
	 * @return the index name
	 */
	private static String getIndexName(XClass clazz, SearchConfiguration cfg) {
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

	/**
	 * Returns an array of index properties.
	 *
	 * Properties are defaulted. For a given property name,
	 * hibernate.search.indexname.n has priority over hibernate.search.indexname which has priority over hibernate.search.default
	 * If the Index is not sharded, a single Properties is returned
	 * If the index is sharded, the Properties index matches the shard index
	 *
	 * @return an array of index properties
	 */
	private static Properties[] getIndexProperties(SearchConfiguration cfg, String indexName) {
		Properties rootCfg = new MaskedProperty( cfg.getProperties(), "hibernate.search" );
		Properties globalProperties = new MaskedProperty( rootCfg, "default" );
		Properties directoryLocalProperties = new MaskedProperty( rootCfg, indexName, globalProperties );
		String shardsCountValue = directoryLocalProperties.getProperty( NBR_OF_SHARDS );

		if ( shardsCountValue == null ) {
			// no shard finished.
			return new Properties[] { directoryLocalProperties };
		}
		else {
			// count shards
			int shardsCount = ConfigurationParseHelper.parseInt(
					shardsCountValue, "'" + shardsCountValue + "' is not a valid value for " + NBR_OF_SHARDS
			);

			if ( shardsCount <= 0 ) {
				throw log.getInvalidShardCountException( shardsCount );
			}

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

	private ShardIdentifierProvider createShardIdentifierProvider(WorkerBuildContext buildContext, Properties indexProperty) {
		ShardIdentifierProvider shardIdentifierProvider;
		String shardIdentityProviderName = indexProperty.getProperty( SHARDING_STRATEGY );
		ServiceManager serviceManager = buildContext.getServiceManager();
		shardIdentifierProvider = ClassLoaderHelper.instanceFromName(
				ShardIdentifierProvider.class,
				shardIdentityProviderName,
				"ShardIdentifierProvider",
				serviceManager
		);

		shardIdentifierProvider.initialize( new MaskedProperty( indexProperty, SHARDING_STRATEGY ), buildContext );

		return shardIdentifierProvider;
	}

	private EntityIndexingInterceptor createEntityIndexingInterceptor(XClass entity) {
		Indexed indexedAnnotation = entity.getAnnotation( Indexed.class );
		EntityIndexingInterceptor interceptor = null;
		if ( indexedAnnotation != null ) {
			Class<? extends EntityIndexingInterceptor> interceptorClass = getInterceptorClassFromHierarchy(
					entity,
					indexedAnnotation
			);
			if ( interceptorClass == EntityIndexingInterceptor.class ) {
				interceptor = null;
			}
			else {
				interceptor = ClassLoaderHelper.instanceFromClass(
						EntityIndexingInterceptor.class,
						interceptorClass,
						"IndexingActionInterceptor for " + entity.getName()
				);
			}
		}
		return interceptor;
	}

	private Similarity createSimilarity(String directoryProviderName,
			SearchConfiguration searchConfiguration,
			Properties indexProperties,
			XClass clazz,
			WorkerBuildContext buildContext) {

		// now we check the config
		Similarity configLevelSimilarity = getConfiguredPerIndexSimilarity(
				directoryProviderName,
				indexProperties,
				buildContext
		);

		if ( configLevelSimilarity != null ) {
			return configLevelSimilarity;
		}
		else {
			return getDefaultSimilarity( searchConfiguration, buildContext );
		}
	}

	private Similarity getDefaultSimilarity(SearchConfiguration searchConfiguration, WorkerBuildContext buildContext) {
		String defaultSimilarityClassName = searchConfiguration.getProperty( Environment.SIMILARITY_CLASS );
		if ( StringHelper.isEmpty( defaultSimilarityClassName ) ) {
			return DEFAULT_SIMILARITY;
		}
		else {
			ServiceManager serviceManager = buildContext.getServiceManager();
			return ClassLoaderHelper.instanceFromName(
					Similarity.class,
					defaultSimilarityClassName,
					"default similarity",
					serviceManager
			);

		}
	}

	private Similarity getConfiguredPerIndexSimilarity(String directoryProviderName, Properties indexProperties, WorkerBuildContext buildContext) {
		Similarity configLevelSimilarity = null;
		String similarityClassName = indexProperties.getProperty( Environment.SIMILARITY_CLASS_PER_INDEX );
		if ( similarityClassName != null ) {
			ServiceManager serviceManager = buildContext.getServiceManager();
			configLevelSimilarity = ClassLoaderHelper.instanceFromName(
					Similarity.class,
					similarityClassName,
					"Similarity class for index " + directoryProviderName,
					serviceManager
			);
		}
		return configLevelSimilarity;
	}

	private IndexShardingStrategy createIndexShardingStrategy(Properties[] indexProps,
			IndexManager[] indexManagers,
			WorkerBuildContext buildContext) {
		IndexShardingStrategy shardingStrategy;

		// any indexProperty will do, the indexProps[0] surely exists.
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
			ServiceManager serviceManager = buildContext.getServiceManager();
			shardingStrategy = ClassLoaderHelper.instanceFromName(
					IndexShardingStrategy.class,
					shardingStrategyName,
					"IndexShardingStrategy",
					serviceManager
			);
		}
		shardingStrategy.initialize(
				new MaskedProperty( indexProps[0], SHARDING_STRATEGY ), indexManagers
		);
		return shardingStrategy;
	}

	private IndexManager[] createIndexManagers(String indexBaseName,
			Properties[] indexProperties,
			Similarity similarity,
			Class<?> mappedClass,
			WorkerBuildContext context) {
		IndexManager[] indexManagers;
		int nbrOfIndexManagers = indexProperties.length;
		indexManagers = new IndexManager[nbrOfIndexManagers];
		for ( int index = 0; index < nbrOfIndexManagers; index++ ) {
			String indexManagerName = nbrOfIndexManagers > 1 ?
					indexBaseName + INDEX_SHARD_ID_SEPARATOR + index :
					indexBaseName;
			Properties indexProp = indexProperties[index];
			IndexManager indexManager = indexManagersRegistry.get( indexManagerName );
			if ( indexManager == null ) {
				indexManager = createIndexManager(
						indexManagerName, mappedClass, similarity, indexProp, context
				);
			}
			else {
				if ( !indexManager.getSimilarity().getClass().equals( similarity.getClass() ) ) {
					throw log.getMultipleEntitiesShareIndexWithInconsistentSimilarityException(
							mappedClass.getName(),
							similarity.getClass().getName(),
							indexManager.getContainedTypes().iterator().next().getName(),
							indexManager.getSimilarity().getClass().getName()
					);
				}
				indexManager.addContainedEntity( mappedClass );
			}
			indexManagers[index] = indexManager;
		}
		return indexManagers;
	}

	/**
	 * Clients of this method should first optimistically check the indexManagersRegistry, which might already contain the needed IndexManager,
	 * to avoid contention on this synchronized method during dynamic reconfiguration at runtime.
	 */
	private synchronized IndexManager createIndexManager(String indexManagerName,
			Class<?> mappedClass,
			Similarity similarity,
			Properties indexProperties,
			WorkerBuildContext context) {
		IndexManager indexManager = indexManagersRegistry.get( indexManagerName );
		if ( indexManager == null ) {
			indexManager = createIndexManager(
					indexManagerName,
					similarity,
					indexProperties,
					context
			);
			indexManagersRegistry.put( indexManagerName, indexManager );
		}
		indexManager.addContainedEntity( mappedClass );
		return indexManager;
	}

	private boolean isShardingDynamic(Properties indexProperty, WorkerBuildContext buildContext) {
		boolean isShardingDynamic = false;

		String shardingStrategyName = indexProperty.getProperty( SHARDING_STRATEGY );
		if ( shardingStrategyName == null ) {
			return isShardingDynamic;
		}

		Class<?> shardingStrategy;
		try {
			shardingStrategy = ClassLoaderHelper.classForName(
					shardingStrategyName,
					buildContext.getServiceManager()
			);
		}
		catch (ClassLoadingException e) {
			throw log.getUnableToLoadShardingStrategyClassException( shardingStrategyName );
		}

		if ( ShardIdentifierProvider.class.isAssignableFrom( shardingStrategy ) ) {
			isShardingDynamic = true;
		}

		return isShardingDynamic;
	}
}
