/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.util.Properties;
import java.io.Serializable;

import org.apache.lucene.document.Document;

import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This implementation use idInString as the hashKey.
 *
 * @author Emmanuel Bernard
 */
public class IdHashShardingStrategy implements IndexShardingStrategy {
	private static final Log log = LoggerFactory.make();

	private IndexManager[] indexManagers;

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
		if ( indexManagers.length == 1 ) {
			log.idHashShardingWithSingleShard();
		}
		this.indexManagers = indexManagers;
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		return indexManagers;
	}

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return indexManagers[hashKey( idInString )];
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		if ( idInString == null ) {
			return indexManagers;
		}
		return new IndexManager[] { indexManagers[hashKey( idInString )] };
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return getIndexManagersForAllShards();
	}

	private int hashKey(String key) {
		// reproduce the hashCode implementation of String as documented in the javadoc
		// to be safe cross Java version (in case it changes some day)
		int hash = 0;
		int length = key.length();
		for ( int index = 0; index < length; index++ ) {
			hash = 31 * hash + key.charAt( index );
		}
		return Math.abs( hash % indexManagers.length );
	}
}
