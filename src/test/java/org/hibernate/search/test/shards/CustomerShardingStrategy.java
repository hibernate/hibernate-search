package org.hibernate.search.test.shards;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;

import org.hibernate.search.FullTextFilter;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Shards an index containing data for multiple customers by customerID. customerID is
 * provided as a property on all indexes entities, and is also defined as a Filter.
 * 
 * The number of shards should be configured to be MAX(customerID).
 *
 * @author Chase Seibert
 */
public class CustomerShardingStrategy implements IndexShardingStrategy {

	// stored DirectoryProviders in a array indexed by customerID
	private DirectoryProvider<?>[] providers;
	
	public void initialize(Properties properties, DirectoryProvider<?>[] providers) {
		this.providers = providers;
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForAllShards() {
		return providers;
	}

	public DirectoryProvider<?> getDirectoryProviderForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		Integer customerID = Integer.parseInt(document.getField("customerID").stringValue());
		return providers[customerID];
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return getDirectoryProvidersForAllShards();
	}

	/**
	 * Optimization; don't search ALL shards and union the results; in this case, we 
	 * can be certain that all the data for a particular customer Filter is in a single
	 * shard; simply return that shard by customerID.
	 */
	public DirectoryProvider<?>[] getDirectoryProvidersForQuery(FullTextFilterImplementor[] filters) {
		FullTextFilter filter = getCustomerFilter(filters, "customer");
		if (filter == null) {
			return getDirectoryProvidersForAllShards();
		}
		else {
			return new DirectoryProvider[] { providers[Integer.parseInt(filter.getParameter("customerID").toString())] };
		}
	}

	private FullTextFilter getCustomerFilter(FullTextFilterImplementor[] filters, String name) {
		for (FullTextFilterImplementor filter: filters) {
			if (filter.getName().equals(name)) return filter;
		}
		return null;
	}

}
