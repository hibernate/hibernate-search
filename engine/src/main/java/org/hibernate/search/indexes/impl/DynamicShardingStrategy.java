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
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class DynamicShardingStrategy implements IndexShardingStrategy {
	private final ShardIdentifierProvider shardIdentifierProvider;
	private final IndexManagerGroupHolder indexManagerGroupHolder;
	private final Properties indexProperties;
	private final IndexedTypeIdentifier entityType;

	public DynamicShardingStrategy(ShardIdentifierProvider shardIdentifierProvider,
			IndexManagerGroupHolder indexManagerGroupHolder, Properties indexProperties, IndexedTypeIdentifier entityType) {
		this.shardIdentifierProvider = shardIdentifierProvider;
		this.indexManagerGroupHolder = indexManagerGroupHolder;
		this.indexProperties = indexProperties;
		this.entityType = entityType;
	}

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		Set<String> allShardIdentifiers = shardIdentifierProvider.getAllShardIdentifiers();
		return getIndexManagersFromShards( allShardIdentifiers );
	}

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		String shardIdentifier = shardIdentifierProvider.getShardIdentifier( entity, id, idInString, document );
		return indexManagerGroupHolder.getOrCreateIndexManager( shardIdentifier, indexProperties, entityType, null );
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		Set<String> shardIdentifiers = shardIdentifierProvider.getShardIdentifiersForDeletion( entity, id, idInString );
		return getIndexManagersFromShards( shardIdentifiers );
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		Set<String> shards = shardIdentifierProvider.getShardIdentifiersForQuery( fullTextFilters );
		return getIndexManagersFromShards( shards );
	}

	public ShardIdentifierProvider getShardIdentifierProvider() {
		return shardIdentifierProvider;
	}

	private IndexManager[] getIndexManagersFromShards(Set<String> shardIdentifiers) {
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
		return managers.toArray( new IndexManager[shardIdentifiers.size()] );
	}
}


