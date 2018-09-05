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
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.impl.ClassLoaderHelper;


/**
 * @author Yoann Rodiere
 */
class DynamicShardingEntityIndexBinder implements EntityIndexBinder {

	private final Class<?> shardIdentifierProviderClass;
	private final Properties properties;

	public DynamicShardingEntityIndexBinder(Class<?> shardIdentifierProviderClass, Properties properties) {
		this.shardIdentifierProviderClass = shardIdentifierProviderClass;
		this.properties = properties;
	}

	@Override
	public MutableEntityIndexBinding bind(IndexManagerGroupHolder holder, IndexedTypeIdentifier entityType,
			EntityIndexingInterceptor<?> interceptor, WorkerBuildContext buildContext) {
		Properties maskedProperties = new MaskedProperty( properties, IndexManagerHolder.SHARDING_STRATEGY );
		ShardIdentifierProvider shardIdentifierProvider = createShardIdentifierProvider(
				buildContext, maskedProperties
		);
		DynamicShardingIndexManagerSelector indexManagerSelector =
				new DynamicShardingIndexManagerSelector( shardIdentifierProvider, holder, properties, entityType );

		/*
		 * Ensure the backend is created even if there are no indexes yet:
		 * this allows master/slave backends to set up message consumers
		 * on the master node in case a slave initiates the creation
		 * of an index, for instance.
		 */
		preInitializeBackend( holder, buildContext );

		return new MutableEntityIndexBinding( holder, indexManagerSelector, shardIdentifierProvider, interceptor );
	}

	@Override
	public String createBackendIdentifier(String backendName, String indexName) {
		/*
		 * There will be only one backend instance shared among all shards.
		 *
		 * We only integrate the backend name in the ID, in order to
		 * handle the case where the backend delegates to another implementation.
		 */
		return backendName;
	}

	private ShardIdentifierProvider createShardIdentifierProvider(WorkerBuildContext buildContext, Properties indexProperty) {
		ShardIdentifierProvider shardIdentifierProvider = ClassLoaderHelper.instanceFromClass(
				ShardIdentifierProvider.class,
				shardIdentifierProviderClass,
				"ShardIdentifierProvider"
		);

		shardIdentifierProvider.initialize( properties, buildContext );

		return shardIdentifierProvider;
	}

	private void preInitializeBackend(IndexManagerGroupHolder holder, WorkerBuildContext buildContext) {
		/*
		 * We only rely on the index manager name for debugging purposes:
		 * see createBackendIdentifier, the index manager name is not used in the backend identifier.
		 *
		 * Thus we can safely assume that the index manager name is the index name base.
		 */
		holder.getOrCreateBackend( holder.getIndexNameBase(), properties, buildContext );
	}

}
