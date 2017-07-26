/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.Closer;
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
	static final String SHARDING_STRATEGY = "sharding_strategy";
	private static final String NBR_OF_SHARDS = SHARDING_STRATEGY + ".nbr_of_shards";

	private static final Similarity DEFAULT_SIMILARITY = new ClassicSimilarity();

	private static final String DEFAULT_INDEX_MANAGER_KEY = "__DEFAULT__";

	private final ConcurrentMap<String, IndexManagerType> indexManagerImplementationsRegistry = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, IndexManager> indexManagersRegistry = new ConcurrentHashMap<String, IndexManager>();
	private final ConcurrentMap<String, BackendQueueProcessor> backendQueueProcessorRegistry = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, IndexManagerGroupHolder> groupHolderRegistry = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, IndexManagerGroupHolder> groupHolderByIndexManagerNameRegistry = new ConcurrentHashMap<>();

	private ExtendedSearchIntegrator integrator;

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
			IndexedTypeIdentifier mappedClassId,
			SearchConfiguration cfg,
			WorkerBuildContext buildContext
	) {
		String indexName = getIndexName( entity, cfg );
		IndexManagerGroupHolder groupHolder = getOrCreateGroupHolder(
				indexName, cfg, buildContext );

		EntityIndexingInterceptor<?> interceptor = createEntityIndexingInterceptor( entity );

		return groupHolder.bind( mappedClassId, interceptor, buildContext );
	}

	private synchronized IndexManagerGroupHolder getOrCreateGroupHolder(
			String indexNameBase, SearchConfiguration cfg, WorkerBuildContext buildContext) {
		IndexManagerGroupHolder holder = groupHolderRegistry.get( indexNameBase );
		if ( holder != null ) {
			return holder;
		}

		Properties[] properties = getIndexProperties( cfg, indexNameBase );
		Similarity similarity = createSimilarity( indexNameBase, cfg, properties[0], buildContext );
		boolean isDynamicSharding = isShardingDynamic( properties[0], buildContext );

		IndexManagerType indexManagerType = getIndexManagerType( indexNameBase, properties, cfg, buildContext );

		EntityIndexBinder entityIndexBinder = null;
		if ( isDynamicSharding ) {
			entityIndexBinder = createDynamicShardingEntityIndexBinder( properties, buildContext );
		}
		else {
			entityIndexBinder = createNonDynamicShardingEntityIndexBinder( properties, buildContext );
		}

		holder = new IndexManagerGroupHolder( this, indexNameBase,
				similarity, indexManagerType, entityIndexBinder );

		groupHolderRegistry.put( indexNameBase, holder );

		return holder;
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
	 * @param integrator the new SearchIntegrator to set on each IndexManager.
	 */
	public void setActiveSearchIntegrator(ExtendedSearchIntegrator integrator) {
		this.integrator = integrator;
		SearchException exception = null;
		for ( IndexManager indexManager : getIndexManagers() ) {
			try {
				indexManager.setSearchFactory( integrator );
			}
			catch (SearchException e) {
				if ( exception == null ) {
					exception = e;
				}
				else {
					exception.addSuppressed( e );
				}
			}
		}
		if ( exception != null ) {
			throw exception;
		}
	}

	ExtendedSearchIntegrator getActiveSearchIntegrator() {
		return integrator;
	}

	void register(String indexName, IndexManagerGroupHolder groupHolder) {
		groupHolderByIndexManagerNameRegistry.put( indexName, groupHolder );
	}

	void register(String indexName, IndexManager indexManager, BackendQueueProcessor backendQueueProcessor) {
		indexManagersRegistry.put( indexName, indexManager );
		backendQueueProcessorRegistry.put( indexName, backendQueueProcessor );
	}

	/**
	 * Stops all IndexManager instances
	 */
	public synchronized void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BackendQueueProcessor::close, backendQueueProcessorRegistry.values() );
			backendQueueProcessorRegistry.clear();
			closer.pushAll( IndexManagerGroupHolder::close, groupHolderRegistry.values() );
			groupHolderRegistry.clear();
			groupHolderByIndexManagerNameRegistry.clear();
			indexManagersRegistry.clear();
			indexManagerImplementationsRegistry.clear();
		}
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

	public BackendQueueProcessor getBackendQueueProcessor(String indexName) {
		if ( indexName == null ) {
			throw log.nullIsInvalidIndexName();
		}

		return backendQueueProcessorRegistry.get( indexName );
	}

	public IndexManagerGroupHolder getGroupHolderByIndexManager(IndexManager indexManager) {
		String indexName = indexManager.getIndexName();
		IndexManagerGroupHolder groupHolder = groupHolderByIndexManagerNameRegistry.get( indexName );
		if ( groupHolder == null ) {
			throw new AssertionFailure( "An index manager was not properly registered in the IndexManagerHolder: " + indexName );
		}
		return groupHolder;
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

	private EntityIndexBinder createDynamicShardingEntityIndexBinder(Properties indexProperty[], WorkerBuildContext buildContext) {
		String shardIdentityProviderName = indexProperty[0].getProperty( SHARDING_STRATEGY );
		ServiceManager serviceManager = buildContext.getServiceManager();
		Class<? extends ShardIdentifierProvider> shardIdentifierProviderClass = ClassLoaderHelper.classForName(
				ShardIdentifierProvider.class,
				shardIdentityProviderName,
				"ShardIdentifierProvider",
				serviceManager
		);

		return new DynamicShardingEntityIndexBinder( shardIdentifierProviderClass, indexProperty[0] );
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

	private EntityIndexBinder createNonDynamicShardingEntityIndexBinder(Properties[] indexProps,
			WorkerBuildContext buildContext) {
		Class<? extends IndexShardingStrategy> shardingStrategyClass;

		// any indexProperty will do, the indexProps[0] surely exists.
		String shardingStrategyName = indexProps[0].getProperty( SHARDING_STRATEGY );
		if ( shardingStrategyName == null ) {
			if ( indexProps.length == 1 ) {
				return new NotShardedEntityIndexBinder( indexProps[0] );
			}
			else {
				shardingStrategyClass = IdHashShardingStrategy.class;
			}
		}
		else {
			ServiceManager serviceManager = buildContext.getServiceManager();
			shardingStrategyClass = ClassLoaderHelper.classForName(
					IndexShardingStrategy.class,
					shardingStrategyName,
					"IndexShardingStrategy",
					serviceManager
			);
		}

		return new NonDynamicShardingEntityIndexBinder( shardingStrategyClass, indexProps );
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

	public synchronized IndexManagerType getIndexManagerType(String indexName, Properties[] indexProperties,
			SearchConfiguration cfg, WorkerBuildContext buildContext) {
		ServiceManager serviceManager = buildContext.getServiceManager();

		//TODO the following code assumes that all shards use the same type;
		//we decided to commit on this limitation by design, yet it's not being validated at this point.
		String indexManagerImplementationName = indexProperties[0].getProperty( Environment.INDEX_MANAGER_IMPL_NAME );

		String indexManagerImplementationKey = StringHelper.isEmpty( indexManagerImplementationName ) ?
				DEFAULT_INDEX_MANAGER_KEY : indexManagerImplementationName;
		if ( indexManagerImplementationsRegistry.containsKey( indexManagerImplementationKey ) ) {
			return indexManagerImplementationsRegistry.get( indexManagerImplementationKey );
		}

		final IndexManagerType indexManagerType;
		try ( ServiceReference<IndexManagerFactory> indexManagerFactoryRef
				= serviceManager.requestReference( IndexManagerFactory.class ) ) {
			IndexManagerFactory indexManagerFactory = indexManagerFactoryRef.get();
			indexManagerType = indexManagerFactory.createIndexManagerByName( indexManagerImplementationName )
					.getIndexManagerType();
		}
		indexManagerImplementationsRegistry.put( indexManagerImplementationKey, indexManagerType );
		return indexManagerType;
	}

	public Collection<IndexManagerType> getIndexManagerTypes() {
		return new HashSet<>( indexManagerImplementationsRegistry.values() );
	}
}
