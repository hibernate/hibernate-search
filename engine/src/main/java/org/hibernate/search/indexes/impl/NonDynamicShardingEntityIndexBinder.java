/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.Properties;

import org.hibernate.search.engine.impl.MutableEntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
@SuppressWarnings("deprecation")
class NonDynamicShardingEntityIndexBinder implements EntityIndexBinder {
	private static final Log log = LoggerFactory.make();
	private static final String INDEX_BACKEND_NAME_SEPARATOR = "#";

	private final Class<? extends IndexShardingStrategy> shardingStrategyClass;
	private final Properties[] properties;

	public NonDynamicShardingEntityIndexBinder(Class<? extends IndexShardingStrategy> shardingStrategyClass, Properties[] properties) {
		this.shardingStrategyClass = shardingStrategyClass;
		this.properties = properties;
	}

	@Override
	public MutableEntityIndexBinding bind(IndexManagerGroupHolder holder, IndexedTypeIdentifier entityType,
			EntityIndexingInterceptor<?> interceptor, WorkerBuildContext buildContext) {
		IndexShardingStrategy shardingStrategy = ClassLoaderHelper.instanceFromClass(
				IndexShardingStrategy.class,
				shardingStrategyClass,
				"IndexShardingStrategy"
		);

		IndexManager[] indexManagers = preInitializeIndexManagersAndBackends( holder, entityType, buildContext );

		Properties maskedProperties = new MaskedProperty( properties[0], IndexManagerHolder.SHARDING_STRATEGY );
		shardingStrategy.initialize( maskedProperties, indexManagers );

		IndexShardingStrategyIndexManagerSelector selector =
				new IndexShardingStrategyIndexManagerSelector( shardingStrategy, indexManagers );

		return new MutableEntityIndexBinding( holder, selector, null, interceptor );
	}

	@Override
	public String createBackendIdentifier(String backendName, String indexName) {
		/*
		 * Each shard will have its own backend instances,
		 * because each shard has a separate set of properties.
		 * We also integrate the backend name in the ID, in order to
		 * handle the case where a backend delegates to another implementation.
		 */
		return indexName + INDEX_BACKEND_NAME_SEPARATOR + backendName;
	}

	private IndexManager[] preInitializeIndexManagersAndBackends(IndexManagerGroupHolder holder,
			IndexedTypeIdentifier entityType, WorkerBuildContext context) {
		IndexManager[] indexManagers;
		int nbrOfIndexManagers = properties.length;
		if ( nbrOfIndexManagers == 0 ) {
			throw log.entityWithNoShard( entityType );
		}
		indexManagers = new IndexManager[nbrOfIndexManagers];
		for ( int index = 0; index < nbrOfIndexManagers; index++ ) {
			String shardIdentifier = nbrOfIndexManagers > 1 ? String.valueOf( index ) : null;
			Properties indexProp = properties[index];
			indexManagers[index] = holder.getOrCreateIndexManager( shardIdentifier, indexProp, entityType, context );
		}
		return indexManagers;
	}

}
