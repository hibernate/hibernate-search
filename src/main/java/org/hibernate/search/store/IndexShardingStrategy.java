// $Id$
package org.hibernate.search.store;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;

import org.hibernate.search.filter.FullTextFilterImplementor;

/**
 * Defines how a given virtual index shards data into different DirectoryProviders
 *
 * @author Emmanuel Bernard
 */
public interface IndexShardingStrategy {
	/**
	 * provides access to sharding properties (under the suffix sharding_strategy)
	 * and provide access to all the DirectoryProviders for a given index
	 */
	void initialize(Properties properties, DirectoryProvider<?>[] providers);

	/**
	 * Ask for all shards (eg to query or optimize)
	 */
	DirectoryProvider<?>[] getDirectoryProvidersForAllShards();

	/**
	 * return the DirectoryProvider where the given entity will be indexed
	 */
	DirectoryProvider<?> getDirectoryProviderForAddition(Class<?> entity, Serializable id, String idInString, Document document);
	/**
	 * return the DirectoryProvider(s) where the given entity is stored and where the deletion operation needs to be applied
	 * id and idInString can be null. If null, all the directory providers containing entity types should be returned
	 */
	DirectoryProvider<?>[] getDirectoryProvidersForDeletion(Class<?> entity, Serializable id, String idInString);

	/**
	 * return the set of DirectoryProvider(s) where the entities matching the filters are stored
	 * this optional optimization allows queries to hit a subset of all shards, which may be useful for some datasets
	 * if this optimization is not needed, return getDirectoryProvidersForAllShards()
	 *
	 * fullTextFilters can be empty if no filter is applied
	 */
	DirectoryProvider<?>[] getDirectoryProvidersForQuery(FullTextFilterImplementor[] fullTextFilters);
}
