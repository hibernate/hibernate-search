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
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ProductsAvailabilityShardingStrategy implements IndexShardingStrategy {

	private IndexManager availableProductsIndex;
	private IndexManager toOrderProductsIndex;
	private IndexManager[] both;

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
		availableProductsIndex = indexManagers[0];
		toOrderProductsIndex = indexManagers[1];
		both = indexManagers;
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		return both;
	}

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		String isAvailable = document.get( "available" );
		if ( "true".equals( isAvailable ) ) {
			return availableProductsIndex;
		}
		else {
			return toOrderProductsIndex;
		}
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return both;
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return both;
	}

}
