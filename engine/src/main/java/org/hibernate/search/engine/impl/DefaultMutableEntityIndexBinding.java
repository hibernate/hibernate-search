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
package org.hibernate.search.engine.impl;

import java.util.Set;

import org.apache.lucene.search.Similarity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DefaultMutableEntityIndexBinding<T> implements MutableEntityIndexBinding<T> {

	private final IndexShardingStrategy shardingStrategy;
	private final Similarity similarityInstance;
	private DocumentBuilderIndexedEntity<T> documentBuilder;
	private final IndexManager[] indexManagers;
	private final EntityIndexingInterceptor entityIndexingInterceptor;

	public DefaultMutableEntityIndexBinding(
			IndexShardingStrategy shardingStrategy,
			Similarity similarityInstance,
			IndexManager[] providers,
			EntityIndexingInterceptor<? super T> entityIndexingInterceptor) {
				this.shardingStrategy = shardingStrategy;
				this.similarityInstance = similarityInstance;
				this.indexManagers = providers;
				this.entityIndexingInterceptor = entityIndexingInterceptor;
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
		return null;
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
		return indexManagers;
	}

	@Override
	public EntityIndexingInterceptor getEntityIndexingInterceptor() {
		return entityIndexingInterceptor;
	}

}
