/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.indexes.impl.DynamicShardingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerGroupHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.ShardIdentifierProvider;

/**
 * @author Emmanuel Bernard
 */
public class DynamicShardingEntityIndexBinding extends AbstractMutableEntityIndexBinding {

	private final DynamicShardingStrategy shardingStrategy;

	public DynamicShardingEntityIndexBinding(
			IndexManagerGroupHolder indexManagerGroupHolder,
			DynamicShardingStrategy shardingStrategy,
			EntityIndexingInterceptor<?> entityIndexingInterceptor) {
		super( indexManagerGroupHolder, entityIndexingInterceptor );
		this.shardingStrategy = shardingStrategy;
	}

	@Override
	public DynamicShardingStrategy getSelectionStrategy() {
		return shardingStrategy;
	}

	@Override
	public ShardIdentifierProvider getShardIdentifierProvider() {
		return shardingStrategy.getShardIdentifierProvider();
	}

	@Override
	public IndexManager[] getIndexManagers() {
		return shardingStrategy.getIndexManagersForAllShards();
	}

}
