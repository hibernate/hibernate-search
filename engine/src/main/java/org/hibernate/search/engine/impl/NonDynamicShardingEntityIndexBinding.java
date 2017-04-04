/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.indexes.impl.IndexManagerGroupHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
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
			IndexManagerGroupHolder indexManagerGroupHolder,
			IndexShardingStrategy shardingStrategy,
			IndexManager[] indexManagers,
			EntityIndexingInterceptor<?> entityIndexingInterceptor) {
		super( indexManagerGroupHolder, entityIndexingInterceptor );
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
