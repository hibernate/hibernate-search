/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;

import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
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

	// stored IndexManagers in a array indexed by customerID
	private IndexManager[] indexManagers;

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
		this.indexManagers = indexManagers;
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		return indexManagers;
	}

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		final String stringValueId = document.getField( "customerID" ).stringValue();
		final Integer customerID = Integer.parseInt( stringValueId );
		return indexManagers[customerID];
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return getIndexManagersForAllShards();
	}

	/**
	 * Optimization; don't search ALL shards and union the results; in this case, we
	 * can be certain that all the data for a particular customer Filter is in a single
	 * shard; simply return that shard by customerID.
	 */
	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] filters) {
		FullTextFilter filter = getCustomerFilter( filters, "customer" );
		if ( filter == null ) {
			return getIndexManagersForAllShards();
		}
		else {
			return new IndexManager[] { indexManagers[Integer.parseInt( filter.getParameter( "customerID" ).toString() )] };
		}
	}

	private FullTextFilter getCustomerFilter(FullTextFilterImplementor[] filters, String name) {
		for ( FullTextFilterImplementor filter: filters ) {
			if ( filter.getName().equals( name ) ) {
				return filter;
			}
		}
		return null;
	}

}
