/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.impl;

import java.util.Set;

import org.apache.lucene.search.Similarity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Hardy Ferentschik
 * @deprecated Will be removed when {@link EntityIndexBinder} gets removed
 */
@Deprecated
public class EntityIndexBindingWrapper implements EntityIndexBinder {
	private final EntityIndexBinding entityIndexBinding;

	public EntityIndexBindingWrapper(EntityIndexBinding entityIndexBinding) {
		this.entityIndexBinding = entityIndexBinding;
	}

	@Override
	public Similarity getSimilarity() {
		return entityIndexBinding.getSimilarity();
	}

	@Override
	public IndexShardingStrategy getSelectionStrategy() {
		return entityIndexBinding.getSelectionStrategy();
	}

	@Override
	public ShardIdentifierProvider getShardIdentifierProvider() {
		return null;
	}

	@Override
	public DocumentBuilderIndexedEntity<?> getDocumentBuilder() {
		return entityIndexBinding.getDocumentBuilder();
	}

	@Override
	public FieldCacheCollectorFactory getIdFieldCacheCollectionFactory() {
		return entityIndexBinding.getIdFieldCacheCollectionFactory();
	}

	@Override
	public void postInitialize(Set<Class<?>> indexedClasses) {
		entityIndexBinding.postInitialize( indexedClasses );
	}

	@Override
	public IndexManager[] getIndexManagers() {
		return entityIndexBinding.getIndexManagers();
	}

	@Override
	public EntityIndexingInterceptor<?> getEntityIndexingInterceptor() {
		return entityIndexBinding.getEntityIndexingInterceptor();
	}
}


