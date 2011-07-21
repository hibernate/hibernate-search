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
package org.hibernate.search.indexes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.Similarity;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.MutableEntityIndexMapping;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.spi.internals.SearchFactoryImplementorWithShareableState;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.impl.DirectoryProviderFactory;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.store.impl.NotShardedStrategy;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public class IndexManagerFactory {
	
	private static final String SHARDING_STRATEGY = "sharding_strategy";
	private static final String NBR_OF_SHARDS = SHARDING_STRATEGY + ".nbr_of_shards";
	
	private final Map<String, IndexManager> indexManagersRegistry;
	
	public IndexManagerFactory() {
		this.indexManagersRegistry = new HashMap<String, IndexManager>();
	}
	
	/**
	 * Multiple IndexManager might be built for the same entity to implement Sharding.
	 * @return a map of created IndexManagers, having as key the names of each index.
	 */
	//I currently think it's easier to not hide sharding implementations in a custom
	//IndexManager to make it easier to explicitly a)detect duplicates b)start-stop
	//additional Managers as needed from a dynamic sharding implementation, without having
	//to embed the sharding logic in a manager itself.
	//so now we have a real 1:1 relation between Managers and indexes, and the signature for
	//#getReader() will always return a single "naive" IndexReader.
	//So we get better caching too, as the changed indexes change cache keys on a fine-grained basis
	//(for both fieldCaches and cached filters)
	public MutableEntityIndexMapping createIndexManagers(XClass entity, SearchConfiguration cfg,
				WorkerBuildContext context,
				ReflectionManager reflectionManager) {
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
				indexManager = createDirectoryManager(
					providerName, indexProps[index],
					reflectionManager.toClass( entity ),
					context
						);
				indexManagersRegistry.put( providerName, indexManager );
			}
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
		
		return new MutableEntityIndexMapping( shardingStrategy, similarityInstance, providers );
	}
	
	/**
	 * Specifies a custom similarity on an index
	 * @param similarityInstance
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

	//FIXME for now we only build "legacy" DirectoryBasedIndexManager
	private IndexManager createDirectoryManager(String indexName, Properties indexProps, Class<?> entity, WorkerBuildContext context) {
		DirectoryProvider<?> provider = DirectoryProviderFactory.createDirectoryProvider ( indexName, indexProps, context );
		
		DirectoryBasedIndexManager manager = new DirectoryBasedIndexManager( provider );
		manager.initialize( indexName, indexProps, context );
		return manager;
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

	public Collection<IndexManager> getIndexManagers() {
		return indexManagersRegistry.values();
	}

	/**
	 * @param factory
	 */
	public void setActiveSearchFactory(SearchFactoryImplementorWithShareableState factory) {
		for ( IndexManager indexManager : getIndexManagers() ) {
			indexManager.setBoundSearchFactory( factory );
		}
	}

}
