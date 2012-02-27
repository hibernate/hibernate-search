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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.Similarity;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.interceptor.DefaultEntityInterceptor;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.impl.DirectoryProviderFactory;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.store.impl.NotShardedStrategy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Stores references to IndexManager instances, and starts/stops them.
 * Starting IndexManagers happens by creating new EntityIndexBinder instances, while creating the binders
 * if we hit the need for a new IndexManager, or several according to a sharding strategy, the new
 * IndexManagers are started incrementally.
 * Stopping IndexManager can not currently happen decrementally: to stop the IndexManagers all of them
 * are stopped.
 * 
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class IndexManagerHolder {
	
	private static final Log log = LoggerFactory.make();
	private static final String SHARDING_STRATEGY = "sharding_strategy";
	private static final String NBR_OF_SHARDS = SHARDING_STRATEGY + ".nbr_of_shards";
	private static final String DEFAULT_INDEX_MANAGER_NAME = "directory-based";

	private static final Map<String, String> defaultIndexManagerClasses;
	static {
		defaultIndexManagerClasses = new HashMap<String, String>( 3 );
		defaultIndexManagerClasses.put( "", DirectoryBasedIndexManager.class.getName() );
		defaultIndexManagerClasses.put( DEFAULT_INDEX_MANAGER_NAME, DirectoryBasedIndexManager.class.getName() );
		defaultIndexManagerClasses.put( "near-real-time", NRTIndexManager.class.getName() );
	}

	private final Map<String, IndexManager> indexManagersRegistry= new ConcurrentHashMap<String, IndexManager>();

	//I currently think it's easier to not hide sharding implementations in a custom
	//IndexManager to make it easier to explicitly a)detect duplicates b)start-stop
	//additional Managers as needed from a dynamic sharding implementation, without having
	//to embed the sharding logic in a manager itself.
	//so now we have a real 1:1 relation between Managers and indexes, and the signature for
	//#getReader() will always return a single "naive" IndexReader.
	//So we get better caching too, as the changed indexes change cache keys on a fine-grained basis
	//(for both fieldCaches and cached filters)
	public synchronized MutableEntityIndexBinding buildEntityIndexBinding(
			XClass entity,
			Class mappedClass,
			SearchConfiguration cfg,
			WorkerBuildContext context
	) {
		// read the properties
		String directoryProviderName = getDirectoryProviderName( entity, cfg );
		Properties[] indexProps = getDirectoryProperties( cfg, directoryProviderName );

		//set up the IndexManagers
		int nbrOfProviders = indexProps.length;
		IndexManager[] providers = new IndexManager[nbrOfProviders];
		for ( int index = 0; index < nbrOfProviders; index++ ) {
			String providerName = nbrOfProviders > 1 ?
					directoryProviderName + "." + index :
					directoryProviderName;
			IndexManager indexManager = indexManagersRegistry.get( providerName );
			if ( indexManager == null ) {
				indexManager = createDirectoryManager( providerName, indexProps[index], context );
				indexManagersRegistry.put( providerName, indexManager );
			}
			indexManager.addContainedEntity( mappedClass );
			providers[index] = indexManager;
		}

		//define sharding strategy for this entity:
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

		//define the Similarity implementation:
		// warning: it can also be set by an annotation at class level
		final String similarityClassName = indexProps[0].getProperty( Environment.SIMILARITY_CLASS_PER_INDEX );
		Similarity similarityInstance = null;
		if ( similarityClassName != null ) {
			similarityInstance = ClassLoaderHelper.instanceFromName(
					Similarity.class,
					similarityClassName,
					DirectoryProviderFactory.class,
					"Similarity class for index " + directoryProviderName
			);
			for ( IndexManager manager : providers ) {
				setSimilarity( similarityInstance, manager );
			}
		}

		Indexed indexedAnnotation = entity.getAnnotation( Indexed.class );
		EntityIndexingInterceptor<?> interceptor = null;
		if (indexedAnnotation != null) {
			Class<? extends EntityIndexingInterceptor> interceptorClass = getInterceptorClassFromHierarchy(
					entity,
					indexedAnnotation
			);
			if (interceptorClass == DefaultEntityInterceptor.class) {
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
		return buildTypesafeMutableEntityBinder(
				entity.getClass(),
				providers,
				shardingStrategy,
				similarityInstance,
				interceptor
		);
	}

	private Class<? extends EntityIndexingInterceptor> getInterceptorClassFromHierarchy(XClass entity, Indexed indexedAnnotation) {
		Class<? extends EntityIndexingInterceptor> result = indexedAnnotation.interceptor();
		XClass superEntity = entity;
		while ( result == DefaultEntityInterceptor.class ) {
			superEntity = superEntity.getSuperclass();
			//Object.class
			if (superEntity == null) {
				return result;
			}
			Indexed indexAnnForSuperclass = superEntity.getAnnotation( Indexed.class );
			result = indexAnnForSuperclass != null ?
					indexAnnForSuperclass.interceptor() :
					result;
		}
		return result;
	}

	@SuppressWarnings( "unchecked" )
	private <T,U> MutableEntityIndexBinding<T> buildTypesafeMutableEntityBinder(Class<T> type, IndexManager[] providers,
																			  IndexShardingStrategy shardingStrategy,
																			  Similarity similarityInstance,
																			  EntityIndexingInterceptor<U> interceptor) {
		EntityIndexingInterceptor<? super T> safeInterceptor = (EntityIndexingInterceptor<? super T> ) interceptor;
		return new MutableEntityIndexBinding<T>( shardingStrategy, similarityInstance, providers, safeInterceptor );
	}

	/**
	 * Specifies a custom similarity on an index
	 * @param newSimilarity
	 * @param manager
	 */
	private void setSimilarity(Similarity newSimilarity, IndexManager manager) {
		Similarity similarity = manager.getSimilarity();
		if ( similarity != null && ! similarity.getClass().equals( newSimilarity.getClass() ) ) {
			throw new SearchException(
					"Multiple entities are sharing the same index but are declaring an " +
							"inconsistent Similarity. When overriding default Similarity make sure that all types sharing a same index " +
							"declare the same Similarity implementation."
			);
		}
		manager.setSimilarity( newSimilarity );
	}

	private IndexManager createDirectoryManager(String indexName, Properties indexProps, WorkerBuildContext context) {
		String indexManagerName = indexProps.getProperty( Environment.INDEX_MANAGER_IMPL_NAME, DEFAULT_INDEX_MANAGER_NAME);
		final IndexManager manager;
		if ( StringHelper.isEmpty( indexManagerName ) ) {
			manager = new DirectoryBasedIndexManager();
		}
		else {
			String longName = defaultIndexManagerClasses.get( indexManagerName );
			if ( longName == null ) {
				longName = indexManagerName;
			}
			manager = ClassLoaderHelper.instanceFromName( IndexManager.class, longName,
						IndexManagerHolder.class, "index manager" );
		}
		try {
			manager.initialize( indexName, indexProps, context );
			return manager;
		} catch (Exception e) {
			throw log.unableToInitializeIndexManager( indexName, e );
		}
	}

	/**
	 * Extracts the index name used for the entity from it's annotations
	 * @return the index name
	 */
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

	/**
	 * @return all IndexManager instances
	 */
	public Collection<IndexManager> getIndexManagers() {
		return indexManagersRegistry.values();
	}

	/**
	 * Useful for MutableSearchFactory, this haves all managed IndexManagers
	 * switch over to the new SearchFactory.
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
	 * @return the IndexManager, or null if it doesn't exist
	 */
	public IndexManager getIndexManager(String targetIndexName) {
		if(targetIndexName == null) {
			throw log.nullIsInvalidIndexName();
		}
		return indexManagersRegistry.get( targetIndexName );
	}
}
