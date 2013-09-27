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
package org.hibernate.search.engine.spi;

import java.util.Set;

import org.apache.lucene.search.Similarity;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * Specifies the relation and options from an indexed entity to its index(es).
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public interface EntityIndexBinding {

	/**
	 * @return the {@code Similarity} used to search and index this entity
	 */
	Similarity getSimilarity();

	/**
	 * @return the sharding strategy
	 */
	IndexShardingStrategy getSelectionStrategy();

	/**
	 * @return the shard identifier provider. Can be {@code null} depending on selected {@code IndexShardingStrategy}.
	 */
	ShardIdentifierProvider getShardIdentifierProvider();

	/**
	 * @return the document builder for this binding
	 */
	DocumentBuilderIndexedEntity<?> getDocumentBuilder();

	/**
	 * @return factory for the field caches
	 */
	FieldCacheCollectorFactory getIdFieldCacheCollectionFactory();

	/**
	 * Called once during bootstrapping
	 *
	 * @param indexedClasses set of indexed classes
	 */
	void postInitialize(Set<Class<?>> indexedClasses);

	/**
	 * @return the array of index managers
	 */
	IndexManager[] getIndexManagers();

	/**
	 * @return the interceptor for indexing operations. Can be {@code null}
	 */
	EntityIndexingInterceptor<?> getEntityIndexingInterceptor();
}
