/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class EntityIndexBindingFactory {

	private static final Log log = LoggerFactory.make();

	private EntityIndexBindingFactory() {
		// not allowed
	}

	@SuppressWarnings( "unchecked" )
	public static <T, U> MutableEntityIndexBinding<T> buildEntityIndexBinding(Class<T> type, IndexManager[] providers,
			IndexShardingStrategy shardingStrategy,
			ShardIdentifierProvider shardIdentifierProvider,
			Similarity similarity,
			EntityIndexingInterceptor<U> interceptor,
			boolean isDynamicSharding,
			Properties properties,
			String rootDirectoryProviderName,
			WorkerBuildContext context,
			IndexManagerHolder indexManagerHolder) {
		if ( !isDynamicSharding && providers.length == 0 ) {
			throw log.entityWithNoShard( type );
		}
		EntityIndexingInterceptor<? super T> safeInterceptor = (EntityIndexingInterceptor<? super T>) interceptor;
		if ( isDynamicSharding ) {
			return new DynamicShardingEntityIndexBinding<T>( shardIdentifierProvider,
					similarity,
					safeInterceptor,
					properties,
					context.getUninitializedSearchFactory(),
					indexManagerHolder,
					rootDirectoryProviderName );
		}
		else {
			return new DefaultMutableEntityIndexBinding<T>( shardingStrategy, similarity, providers, safeInterceptor );
		}
	}
}
