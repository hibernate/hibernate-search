/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.engine.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Similarity;
import org.apache.poi.ss.formula.functions.T;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DynamicShardingEntityIndexBinding<T> implements MutableEntityIndexBinding<T> {

	private final IndexShardingStrategy shardingStrategy;
	private final Similarity similarityInstance;
	private final ShardIdentifierProvider shardIdentityProvider;
	private final Properties properties;
	private final SearchFactoryImplementor searchFactory;
	private final IndexManagerHolder indexManagerHolder;
	private final String rootDirectoryProviderName;
	private DocumentBuilderIndexedEntity<T> documentBuilder;
	private final EntityIndexingInterceptor entityIndexingInterceptor;
	private IndexManagerFactory indexManagerFactory;

	/**
	 * @param shardingStrategy
	 * @param similarityInstance
	 * @param providers
	 */
	public DynamicShardingEntityIndexBinding(
			ShardIdentifierProvider shardIdentityProvider,
			Similarity similarityInstance,
			EntityIndexingInterceptor<? super T> entityIndexingInterceptor,
			Properties properties,
			IndexManagerFactory indexManagerFactory,
			SearchFactoryImplementor searchFactory,
			IndexManagerHolder indexManagerHolder,
			String rootDirectoryProviderName) {
		this.shardIdentityProvider = shardIdentityProvider;
		this.similarityInstance = similarityInstance;
		this.entityIndexingInterceptor = entityIndexingInterceptor;
		this.shardingStrategy = new DynamicShardsShardingStrategy(); //no need to initialize it
		this.properties = properties;
		this.searchFactory = searchFactory;
		this.indexManagerFactory = indexManagerFactory;
		this.indexManagerHolder = indexManagerHolder;
		this.rootDirectoryProviderName = rootDirectoryProviderName;
	}

	public void setDocumentBuilderIndexedEntity(DocumentBuilderIndexedEntity<T> documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	public Properties getProperties() {
		return properties;
	}

	public SearchFactoryImplementor getSearchFactory() {
		return searchFactory;
	}

	@Override
	public Similarity getSimilarity() {
		return similarityInstance;
	}

	@Override
	public IndexShardingStrategy getSelectionStrategy() {
		return shardingStrategy;
	}

	@Override
	public DocumentBuilderIndexedEntity<T> getDocumentBuilder() {
		return documentBuilder;
	}

	@Override
	public FieldCacheCollectorFactory getIdFieldCacheCollectionFactory() {
		//TODO remove this stuff from the DocumentBuilder, bring it here.
		return documentBuilder.getIdFieldCacheCollectionFactory();
	}

	@Override
	public void postInitialize(Set<Class<?>> indexedClasses) {
		documentBuilder.postInitialize( indexedClasses );
	}

	@Override
	public IndexManager[] getIndexManagers() {
		return shardingStrategy.getIndexManagersForAllShards();
	}

	@Override
	public EntityIndexingInterceptor getEntityIndexingInterceptor() {
		return entityIndexingInterceptor;
	}

	public IndexManagerFactory getIndexManagerFactory() {
		return indexManagerFactory;
	}

	public <T> MutableEntityIndexBinding<T> cloneWithSimilarity(Similarity entitySimilarity) {
		return new DynamicShardingEntityIndexBinding<T>(
				shardIdentityProvider,
				entitySimilarity,
				entityIndexingInterceptor,
				properties,
				indexManagerFactory,
				searchFactory,
				indexManagerHolder,
				rootDirectoryProviderName
		);
	}

	private class DynamicShardsShardingStrategy implements IndexShardingStrategy {

		@Override
		public void initialize(Properties properties, IndexManager[] providers) {
		}

		@Override
		public IndexManager[] getIndexManagersForAllShards() {
			String[] shards = shardIdentityProvider.getAllShardIdentifiers();
			return getIndexManagersFromShards( shards );
		}

		@Override
		public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
			String shard = shardIdentityProvider.getShardIdentifier( entity, id, idInString, document );
			return indexManagerHolder.getOrCreateLateIndexManager( getProviderName( shard ), DynamicShardingEntityIndexBinding.this );
		}

		@Override
		public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
			String[] shards = shardIdentityProvider.getShardIdentifiers( entity, id, idInString );
			return getIndexManagersFromShards( shards );
		}

		private IndexManager[] getIndexManagersFromShards(String[] shards) {
			ArrayList<IndexManager> managers = new ArrayList<IndexManager>( shards.length );
			for (String shard : shards) {
				managers.add( indexManagerHolder.getOrCreateLateIndexManager( getProviderName( shard ), DynamicShardingEntityIndexBinding.this ) );
			}
			return managers.toArray(new IndexManager[shards.length]);
		}

		@Override
		public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
			String[] shards = shardIdentityProvider.getShardIdentifiersForQuery( fullTextFilters );
			return getIndexManagersFromShards( shards );
		}

		private final String getProviderName(String shard) {
			return shardIdentityProvider.buildIndexManagerNameFromShard(rootDirectoryProviderName, shard);
		}
	}
}
