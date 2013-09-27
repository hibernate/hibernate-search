/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.store;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Defines how a given virtual index shards data into different IndexManager(s).
 *
 * @author Emmanuel Bernard
 * @deprecated Deprecated as of Hibernate Search 4.4. Might be removed in Search 5. Use {@link ShardIdentifierProvider}
 *             instead.
 */
@Deprecated
public interface IndexShardingStrategy {

	/**
	 * provides access to sharding properties (under the suffix sharding_strategy)
	 * and provide access to all the IndexManager for a given index
	 */
	void initialize(Properties properties, IndexManager[] indexManagers);

	/**
	 * Ask for all shards (eg to query or optimize)
	 */
	IndexManager[] getIndexManagersForAllShards();

	/**
	 * return the IndexManager where the given entity will be indexed
	 */
	IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document);

	/**
	 * return the IndexManager(s) where the given entity is stored and where the deletion operation needs to be applied
	 * id and idInString could be null. If null, all the IndexManagers containing entity types should be returned
	 *
	 * @param entity the type of the deleted entity
	 * @param id the id in object form
	 * @param idInString the id as transformed by the used TwoWayStringBridge
	 */
	IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString);

	/**
	 * return the set of IndexManager(s) where the entities matching the filters are stored
	 * this optional optimization allows queries to hit a subset of all shards, which may be useful for some datasets
	 * if this optimization is not needed, return getIndexManagersForAllShards()
	 *
	 * fullTextFilters can be empty if no filter is applied
	 */
	IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters);
}
