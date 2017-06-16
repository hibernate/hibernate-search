/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
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
	private final IndexManager[] allIndexManagers;

	public IndexShardingStrategyIndexManagerSelector(IndexShardingStrategy shardingStrategy, IndexManager[] allIndexManagers) {
		super();
		this.shardingStrategy = shardingStrategy;
		this.allIndexManagers = allIndexManagers;
	}

	@Override
	public Set<IndexManager> all() {
		IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
		if ( indexManagers == null ) {
			// This is legacy behavior
			indexManagers = allIndexManagers;
		}
		return toSet( indexManagers );
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

	private Set<IndexManager> toSet(IndexManager[] indexManagers) {
		if ( indexManagers == null ) {
			return Collections.emptySet();
		}
		/*
		 * Using a LinkedHashSet because some APIs expose an ordered sequence of index managers.
		 * See for example org.hibernate.search.engine.impl.MutableEntityIndexBinding.getIndexManagers().
		 */
		Set<IndexManager> set = new LinkedHashSet<>( indexManagers.length );
		Collections.addAll( set, indexManagers );
		return set;
	}

}
