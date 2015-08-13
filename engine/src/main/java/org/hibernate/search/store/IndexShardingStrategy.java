/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 *
	 * @param properties configuration properties
	 * @param indexManagers array of {@link IndexManager}
	 */
	void initialize(Properties properties, IndexManager[] indexManagers);

	/**
	 * Ask for all shards (eg to query or optimize)
	 * @return all the {@link IndexManager} for all shards
	 */
	IndexManager[] getIndexManagersForAllShards();

	/**
	 * @param entity the type of the entity
	 * @param id the id in object form
	 * @param idInString the id as transformed by the used TwoWayStringBridge
	 * @param document the document to index
	 * @return the IndexManager where the given entity will be indexed
	 */
	IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document);

	/**
	 * return the IndexManager(s) where the given entity is stored and where the deletion operation needs to be applied
	 * id and idInString could be null. If null, all the IndexManagers containing entity types should be returned
	 *
	 * @param entity the type of the deleted entity
	 * @param id the id in object form
	 * @param idInString the id as transformed by the used TwoWayStringBridge
	 * @return the {@link IndexManager}(s) where the given entity is stored
	 */
	IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString);

	/**
	 * return the set of IndexManager(s) where the entities matching the filters are stored
	 * this optional optimization allows queries to hit a subset of all shards, which may be useful for some datasets
	 * if this optimization is not needed, return getIndexManagersForAllShards()
	 *
	 * @param fullTextFilters can be empty if no filter is applied
	 * @return the set of {@link IndexManager}(s) where the entities matching the filters are stored
	 */
	IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters);
}
