/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.store.IndexShardingStrategy;


/**
 * @author Yoann Rodiere
 */
public class IndexShardingStrategyIndexManagerSelector implements IndexManagerSelector {

	private final IndexShardingStrategy shardingStrategy;
	private final Set<IndexManager> allIndexManagers;

	public IndexShardingStrategyIndexManagerSelector(IndexShardingStrategy shardingStrategy, IndexManager[] allIndexManagers) {
		super();
		this.shardingStrategy = shardingStrategy;
		this.allIndexManagers = convertAll( shardingStrategy, allIndexManagers );
	}

	@Override
	public Set<IndexManager> all() {
		return allIndexManagers;
	}

	@Override
	public IndexManager forNew(IndexedTypeIdentifier typeId, Serializable id, String idInString, Document document) {
		return shardingStrategy.getIndexManagerForAddition( typeId.getPojoType(), id, idInString, document );
	}

	@Override
	public Set<IndexManager> forExisting(IndexedTypeIdentifier typeId, Serializable id, String idInString) {
		return toSet( shardingStrategy.getIndexManagersForDeletion( typeId.getPojoType(), id, idInString ) );
	}

	@Override
	public Set<IndexManager> forFilters(FullTextFilterImplementor[] fullTextFilters) {
		return toSet( shardingStrategy.getIndexManagersForQuery( fullTextFilters ) );
	}

	private static Set<IndexManager> toSet(IndexManager[] indexManagers) {
		if ( indexManagers == null ) {
			return Collections.emptySet();
		}
		Set<IndexManager> set = new HashSet<>( indexManagers.length );
		Collections.addAll( set, indexManagers );
		return set;
	}

	private static Set<IndexManager> convertAll(IndexShardingStrategy shardingStrategy, IndexManager[] allIndexManagers) {
		IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
		if ( indexManagers == null ) {
			// This is legacy behavior
			indexManagers = allIndexManagers;
		}
		return toSet( indexManagers );
	}

}
