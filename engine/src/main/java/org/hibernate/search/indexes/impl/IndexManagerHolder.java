/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.Similarity;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.DynamicShardingEntityIndexBinding;
import org.hibernate.search.engine.impl.EntityIndexBindingFactory;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.indexes.interceptor.DefaultEntityInterceptor;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.store.impl.DirectoryProviderFactory;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.store.impl.NotShardedStrategy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stores references to {@code IndexManager} instances, and starts or stops them.
 *
 * Starting {@code IndexManager}s happens by creating new {@code EntityIndexBinder} instances. {@code IndexManager}s are
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
			WorkerBuildContext context
	) {
		String indexName = getIndexName( entity, cfg );
		Properties[] indexProperties = getIndexProperties( cfg, indexName );
		Similarity similarity = createSimilarity( indexName, cfg, indexProperties[0], entity );
		boolean isDynamicSharding = isShardingDynamic( indexProperties[0] );

		IndexManager[] indexManagers = new IndexManager[0];
		if ( !isDynamicSharding ) {
			indexManagers = createIndexManagers(
					indexName,
					indexProperties,
					similarity,
					mappedClass,
					cfg,

					context
			);
		}

		IndexShardingStrategy shardingStrategy = null;
		if ( !isDynamicSharding ) {
			shardingStrategy = createIndexShardingStrategy( indexProperties, indexManagers );
		}

		ShardIdentifierProvider shardIdentifierProvider = null;
		if ( isDynamicSharding ) {
			shardIdentifierProvider = createShardIdentifierProvider( context, indexProperties[0]
			);
		}

		EntityIndexingInterceptor<?> interceptor = createEntityIndexingInterceptor( entity );

		return EntityIndexBindingFactory.buildEntityIndexBinder(
				entity.getClass(),
				indexManagers,
				shardingStrategy,
				shardIdentifierProvider,
				similarity,
				interceptor,
				isDynamicSharding,
				indexProperties[0],
				indexName,
				context,
				this,
				cfg.getIndexManagerFactory()
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
		SearchFactoryImplementor searchFactory = entityIndexBinding.getSearchFactory();
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
				entityIndexBinding.getIndexManagerFactory(),
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
	 * switch over to the new SearchFactory.
	 *
	 * @param factory the new SearchFactory to set on each IndexManager.
	 */
	public void setActiveSearchFactory(SearchFactoryImplementorWithShareableState factory) {
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
		while ( result == DefaultEntityInterceptor.class ) {
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
			IndexManagerFactory indexManagerFactory,
			Properties properties,
			WorkerBuildContext context) {
		String indexManagerImplementationName = properties.getProperty( Environment.INDEX_MANAGER_IMPL_NAME );
		final IndexManager manager;
		if ( StringHelper.isEmpty( indexManagerImplementationName ) ) {
			manager = indexManagerFactory.createDefaultIndexManager();
		}
		else {
			manager = indexManagerFactory.createIndexManagerByName( indexManagerImplementationName );
		}
		try {
			manager.initialize( indexName, properties, indexSimilarity, context );
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

	private ShardIdentifierProvider createShardIdentifierProvider(WorkerBuildContext context, Properties indexProperty) {
		ShardIdentifierProvider shardIdentifierProvider;
		String shardIdentityProviderName = indexProperty.getProperty( SHARDING_STRATEGY );

		shardIdentifierProvider = ClassLoaderHelper.instanceFromName(
				ShardIdentifierProvider.class,
				shardIdentityProviderName,
				DirectoryProviderFactory.class.getClassLoader(),
				"ShardIdentifierProvider"
		);
		shardIdentifierProvider.initialize( new MaskedProperty( indexProperty, SHARDING_STRATEGY ), context );

		return shardIdentifierProvider;
	}

	private EntityIndexingInterceptor<?> createEntityIndexingInterceptor(XClass entity) {
		Indexed indexedAnnotation = entity.getAnnotation( Indexed.class );
		EntityIndexingInterceptor<?> interceptor = null;
		if ( indexedAnnotation != null ) {
			Class<? extends EntityIndexingInterceptor> interceptorClass = getInterceptorClassFromHierarchy(
					entity,
					indexedAnnotation
			);
			if ( interceptorClass == DefaultEntityInterceptor.class ) {
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

	private Similarity createSimilarity(String directoryProviderName, SearchConfiguration cfg, Properties indexProperties, XClass clazz) {
		// first check on class level
		Similarity classLevelSimilarity = null;

		// TODO - the processing of the @Similarity annotation is temporary here. The annotation should be removed in Search 5 (HF)
		List<XClass> hierarchyClasses = ReflectionHelper.createXClassHierarchy( clazz );
		Class<?> similarityClass = null;
		for ( XClass hierarchyClass : hierarchyClasses ) {
			org.hibernate.search.annotations.Similarity similarityAnnotation = hierarchyClass.getAnnotation( org.hibernate.search.annotations.Similarity.class );
			if ( similarityAnnotation != null ) {
				Class<?> tmpSimilarityClass = similarityAnnotation.impl();
				if ( similarityClass != null && !similarityClass.equals( tmpSimilarityClass ) ) {
					throw log.getMultipleInconsistentSimilaritiesInClassHierarchyException( clazz.getName() );
				}
				else {
					similarityClass = tmpSimilarityClass;
				}
				classLevelSimilarity = ClassLoaderHelper.instanceFromClass(
						Similarity.class,
						similarityClass,
						"Similarity class for index " + directoryProviderName
				);
			}
		}

		// now we check the config
		Similarity configLevelSimilarity = null;
		String similarityClassName = indexProperties.getProperty( Environment.SIMILARITY_CLASS_PER_INDEX );
		if ( similarityClassName != null ) {
			configLevelSimilarity = ClassLoaderHelper.instanceFromName(
					Similarity.class,
					similarityClassName,
					DirectoryProviderFactory.class.getClassLoader(),
					"Similarity class for index " + directoryProviderName
			);
		}

		if ( classLevelSimilarity != null && configLevelSimilarity != null ) {
			throw log.getInconsistentSimilaritySettingBetweenAnnotationsAndConfigPropertiesException(
					classLevelSimilarity.getClass().getName(),
					configLevelSimilarity.getClass().getCanonicalName()
			);
		}
		else if ( classLevelSimilarity != null ) {
			return classLevelSimilarity;
		}
		else if ( configLevelSimilarity != null ) {
			return configLevelSimilarity;
		}
		else {
			String defaultSimilarityClassName = cfg.getProperty( Environment.SIMILARITY_CLASS );
			if ( StringHelper.isEmpty( defaultSimilarityClassName ) ) {
				return Similarity.getDefault();
			}
			else {
				return ClassLoaderHelper.instanceFromName(
						Similarity.class,
						defaultSimilarityClassName,
						ConfigContext.class.getClassLoader(),
						"default similarity"
				);
			}
		}
	}

	private IndexShardingStrategy createIndexShardingStrategy( Properties[] indexProps, IndexManager[] indexManagers ) {
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
			shardingStrategy = ClassLoaderHelper.instanceFromName(
					IndexShardingStrategy.class,
					shardingStrategyName,
					DirectoryProviderFactory.class.getClassLoader(),
					"IndexShardingStrategy"
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
			SearchConfiguration configuration,
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
						indexManagerName, mappedClass, similarity,
						indexProp, configuration.getIndexManagerFactory(), context
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
			IndexManagerFactory indexManagerFactory,
			WorkerBuildContext context) {
		IndexManager indexManager = indexManagersRegistry.get( indexManagerName );
		if ( indexManager == null ) {
			indexManager = createIndexManager(
					indexManagerName,
					similarity,
					indexManagerFactory,
					indexProperties,
					context
			);
			indexManagersRegistry.put( indexManagerName, indexManager );
		}
		indexManager.addContainedEntity( mappedClass );
		return indexManager;
	}

	private boolean isShardingDynamic(Properties indexProperty) {
		boolean isShardingDynamic = false;

		String shardingStrategyName = indexProperty.getProperty( SHARDING_STRATEGY );
		if ( shardingStrategyName == null ) {
			return isShardingDynamic;
		}

		Class<?> shardingStrategy;
		try {
			shardingStrategy = ClassLoaderHelper.classForName(
					shardingStrategyName,
					DirectoryProviderFactory.class.getClassLoader()
			);
		}
		catch (ClassNotFoundException e) {
			throw log.getUnableToLoadShardingStrategyClassException( shardingStrategyName );
		}

		if ( ShardIdentifierProvider.class.isAssignableFrom( shardingStrategy ) ) {
			isShardingDynamic = true;
		}

		return isShardingDynamic;
	}
}
