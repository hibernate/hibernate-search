/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.engine.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
class DynamicShardingStrategy implements IndexShardingStrategy {
	private final ShardIdentifierProvider shardIdentifierProvider;
	private final IndexManagerHolder indexManagerHolder;
	private final String rootIndexName;
	private final DynamicShardingEntityIndexBinding entityIndexBinding;

	DynamicShardingStrategy(ShardIdentifierProvider shardIdentifierProvider,
			IndexManagerHolder indexManagerHolder,
			DynamicShardingEntityIndexBinding entityIndexBinding,
			String rootIndexName) {
		this.shardIdentifierProvider = shardIdentifierProvider;
		this.indexManagerHolder = indexManagerHolder;
		this.entityIndexBinding = entityIndexBinding;
		this.rootIndexName = rootIndexName;
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
		return indexManagerHolder.getOrCreateIndexManager(
				rootIndexName,
				shardIdentifier,
				entityIndexBinding
		);
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		Set<String> shardIdentifiers = shardIdentifierProvider.getAllShardIdentifiers();
		return getIndexManagersFromShards( shardIdentifiers );
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		Set<String> shards = shardIdentifierProvider.getShardIdentifiersForQuery( fullTextFilters );
		return getIndexManagersFromShards( shards );
	}

	ShardIdentifierProvider getShardIdentifierProvider() {
		return shardIdentifierProvider;
	}

	private IndexManager[] getIndexManagersFromShards(Set<String> shardIdentifiers) {
		Set<IndexManager> managers = new HashSet<IndexManager>( shardIdentifiers.size() );
		for ( String shardIdentifier : shardIdentifiers ) {
			managers.add(
					indexManagerHolder.getOrCreateIndexManager(
							rootIndexName,
							shardIdentifier,
							entityIndexBinding
					)
			);
		}
		return managers.toArray( new IndexManager[shardIdentifiers.size()] );
	}
}


