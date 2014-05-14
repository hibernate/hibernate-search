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
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * @author Emmanuel Bernard
 */
public class NotShardedStrategy implements IndexShardingStrategy {

	private IndexManager[] directoryProvider;

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
		this.directoryProvider = indexManagers;
		if ( directoryProvider.length > 1 ) {
			throw new AssertionFailure("Using SingleDirectoryProviderSelectionStrategy with multiple DirectoryProviders");
		}
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		return directoryProvider;
	}

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return directoryProvider[0];
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return directoryProvider;
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return directoryProvider;
	}

}
