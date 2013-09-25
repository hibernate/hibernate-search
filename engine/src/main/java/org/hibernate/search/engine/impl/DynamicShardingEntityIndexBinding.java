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

import java.util.Properties;
import java.util.Set;

import org.apache.lucene.search.Similarity;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DynamicShardingEntityIndexBinding<T> implements MutableEntityIndexBinding<T> {

	private final DynamicShardingStrategy shardingStrategy;
	private final Similarity similarityInstance;
	private final ShardIdentifierProvider shardIdentityProvider;
	private final Properties properties;
	private final SearchFactoryImplementor searchFactory;
	private final IndexManagerHolder indexManagerHolder;
	private final String rootDirectoryProviderName;
	private DocumentBuilderIndexedEntity<T> documentBuilder;
	private final EntityIndexingInterceptor entityIndexingInterceptor;
	private IndexManagerFactory indexManagerFactory;

	public DynamicShardingEntityIndexBinding(
			ShardIdentifierProvider shardIdentityProvider,
			Similarity similarityInstance,
			EntityIndexingInterceptor<? super T> entityIndexingInterceptor,
			Properties properties,
			IndexManagerFactory indexManagerFactory,
			SearchFactoryImplementor searchFactoryImplementor,
			IndexManagerHolder indexManagerHolder,
			String rootDirectoryProviderName) {
		this.shardIdentityProvider = shardIdentityProvider;
		this.similarityInstance = similarityInstance;
		this.entityIndexingInterceptor = entityIndexingInterceptor;
		this.properties = properties;
		this.searchFactory = searchFactoryImplementor;
		this.indexManagerFactory = indexManagerFactory;
		this.indexManagerHolder = indexManagerHolder;
		this.rootDirectoryProviderName = rootDirectoryProviderName;
		this.shardingStrategy = new DynamicShardingStrategy(
				shardIdentityProvider,
				indexManagerHolder,
				this,
				rootDirectoryProviderName
		);
	}

	@Override
	public void setDocumentBuilderIndexedEntity(DocumentBuilderIndexedEntity<T> documentBuilder) {
		this.documentBuilder = documentBuilder;
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
	public ShardIdentifierProvider getShardIdentifierProvider() {
		return shardingStrategy.getShardIdentifierProvider();
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

	public Properties getProperties() {
		return properties;
	}

	public SearchFactoryImplementor getSearchFactory() {
		return searchFactory;
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
}
