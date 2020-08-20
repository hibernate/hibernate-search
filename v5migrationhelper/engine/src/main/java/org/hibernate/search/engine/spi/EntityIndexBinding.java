/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.spi.IndexedTypeSet;
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
	 * @return the index manager selector
	 */
	IndexManagerSelector getIndexManagerSelector();

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
	void postInitialize(IndexedTypeSet indexedClasses);

	/**
	 * @return the type of index managers
	 */
	IndexManagerType getIndexManagerType();

	/**
	 * @return the interceptor for indexing operations. Can be {@code null}
	 */
	EntityIndexingInterceptor getEntityIndexingInterceptor();
}
