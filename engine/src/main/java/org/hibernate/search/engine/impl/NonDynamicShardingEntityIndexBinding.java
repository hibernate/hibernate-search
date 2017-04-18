/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("deprecation")
public class NonDynamicShardingEntityIndexBinding extends AbstractMutableEntityIndexBinding {

	private final IndexShardingStrategy shardingStrategy;
	private final IndexManager[] indexManagers;

	public NonDynamicShardingEntityIndexBinding(
			IndexShardingStrategy shardingStrategy,
			Similarity similarityInstance,
			IndexManagerType indexManagerType,
			IndexManager[] indexManagers,
			EntityIndexingInterceptor<?> entityIndexingInterceptor) {
		super( similarityInstance, indexManagerType, entityIndexingInterceptor );
		this.shardingStrategy = shardingStrategy;
		this.indexManagers = indexManagers;
	}

	@Override
	public IndexShardingStrategy getSelectionStrategy() {
		return shardingStrategy;
	}

	@Override
	public ShardIdentifierProvider getShardIdentifierProvider() {
		return null;
	}

	@Override
	public IndexManager[] getIndexManagers() {
		return indexManagers;
	}

}
