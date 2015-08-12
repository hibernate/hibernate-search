/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

import java.util.Set;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * Specifies the relation and options from an indexed entity to its index(es).
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
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
	DocumentBuilderIndexedEntity getDocumentBuilder();

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
	EntityIndexingInterceptor getEntityIndexingInterceptor();
}
