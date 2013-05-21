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
package org.hibernate.search.test.shards;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;

import org.hibernate.search.FullTextFilter;
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
		Integer customerID = Integer.parseInt( document.getFieldable( "customerID" ).stringValue() );
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
