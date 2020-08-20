/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.indexes.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
class DynamicShardingIndexManagerSelector implements IndexManagerSelector {
	private final ShardIdentifierProvider shardIdentifierProvider;
	private final IndexManagerGroupHolder indexManagerGroupHolder;
	private final Properties indexProperties;
	private final IndexedTypeIdentifier entityType;

	public DynamicShardingIndexManagerSelector(ShardIdentifierProvider shardIdentifierProvider,
			IndexManagerGroupHolder indexManagerGroupHolder, Properties indexProperties, IndexedTypeIdentifier entityType) {
		this.shardIdentifierProvider = shardIdentifierProvider;
		this.indexManagerGroupHolder = indexManagerGroupHolder;
		this.indexProperties = indexProperties;
		this.entityType = entityType;
	}

	@Override
	public Set<IndexManager> all() {
		Set<String> allShardIdentifiers = shardIdentifierProvider.getAllShardIdentifiers();
		return getIndexManagersFromShards( allShardIdentifiers );
	}

	@Override
	public IndexManager forNew(IndexedTypeIdentifier typeId, Serializable id, String idInString, Document document) {
		String shardIdentifier = shardIdentifierProvider.getShardIdentifier( typeId.getPojoType(), id, idInString, document );
		return indexManagerGroupHolder.getOrCreateIndexManager( shardIdentifier, indexProperties, entityType, null );
	}

	@Override
	public Set<IndexManager> forExisting(IndexedTypeIdentifier typeId, Serializable id, String idInString) {
		Set<String> shardIdentifiers = shardIdentifierProvider.getShardIdentifiersForDeletion( typeId.getPojoType(), id, idInString );
		return getIndexManagersFromShards( shardIdentifiers );
	}

	@Override
	public Set<IndexManager> forFilters(FullTextFilterImplementor[] fullTextFilters) {
		Set<String> shards = shardIdentifierProvider.getShardIdentifiersForQuery( fullTextFilters );
		return getIndexManagersFromShards( shards );
	}

	private Set<IndexManager> getIndexManagersFromShards(Set<String> shardIdentifiers) {
		Set<IndexManager> managers = new HashSet<IndexManager>( shardIdentifiers.size() );
		for ( String shardIdentifier : shardIdentifiers ) {
			managers.add(
					indexManagerGroupHolder.getOrCreateIndexManager(
							shardIdentifier,
							indexProperties,
							entityType,
							null
					)
			);
		}
		return managers;
	}
}


