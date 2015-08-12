/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Properties;

import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Build the expected {@link org.hibernate.search.engine.spi.EntityIndexBinding} depending in the configuration.
 *
 * @author Emmanuel Bernard
 */
public final class EntityIndexBindingFactory {

	private static final Log log = LoggerFactory.make();

	private EntityIndexBindingFactory() {
		// not allowed
	}

	@SuppressWarnings( "unchecked" )
	public static MutableEntityIndexBinding buildEntityIndexBinding(Class<?> type, IndexManager[] providers,
			IndexShardingStrategy shardingStrategy,
			ShardIdentifierProvider shardIdentifierProvider,
			Similarity similarity,
			EntityIndexingInterceptor interceptor,
			boolean isDynamicSharding,
			Properties properties,
			String rootDirectoryProviderName,
			WorkerBuildContext context,
			IndexManagerHolder indexManagerHolder) {
		if ( !isDynamicSharding && providers.length == 0 ) {
			throw log.entityWithNoShard( type );
		}
		EntityIndexingInterceptor safeInterceptor = interceptor;
		if ( isDynamicSharding ) {
			return new DynamicShardingEntityIndexBinding( shardIdentifierProvider,
					similarity,
					safeInterceptor,
					properties,
					context.getUninitializedSearchIntegrator(),
					indexManagerHolder,
					rootDirectoryProviderName );
		}
		else {
			return new DefaultMutableEntityIndexBinding( shardingStrategy, similarity, providers, interceptor );
		}
	}
}
