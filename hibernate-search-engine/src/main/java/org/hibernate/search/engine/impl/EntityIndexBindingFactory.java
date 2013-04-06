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

import org.apache.lucene.search.Similarity;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import java.util.Properties;

/**
 * Build the expected {@link EntityIndexBinder} depending in the configuration
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class EntityIndexBindingFactory {

	private static final Log log = LoggerFactory.make();

	@SuppressWarnings( "unchecked" )
	public static <T,U> MutableEntityIndexBinding<T> buildEntityIndexBinder(Class<T> type, IndexManager[] providers,
																			  IndexShardingStrategy shardingStrategy,
																			  Similarity similarityInstance,
																			  EntityIndexingInterceptor<U> interceptor,
																			  boolean isDynamicSharding,
																			  Properties properties) {
		if ( !isDynamicSharding && providers.length == 0 ) {
			throw log.entityWithNoShard( type );
		}
		EntityIndexingInterceptor<? super T> safeInterceptor = (EntityIndexingInterceptor<? super T> ) interceptor;
		if (isDynamicSharding) {
			return null;
		}
		else {
			return new MutableEntityIndexBinding<T>( shardingStrategy, similarityInstance, providers, safeInterceptor );
		}
	}

	@SuppressWarnings( "unchecked" )
	public static <T> MutableEntityIndexBinding<T> copyEntityIndexBindingReplacingSimilarity( Class <T> clazz, EntityIndexBinder entityMapping, Similarity entitySimilarity) {
		EntityIndexingInterceptor<? super T> interceptor = (EntityIndexingInterceptor<? super T> ) entityMapping.getEntityIndexingInterceptor();
		MutableEntityIndexBinding<T> newMapping = new MutableEntityIndexBinding<T>(
				entityMapping.getSelectionStrategy(),
				entitySimilarity,
				entityMapping.getIndexManagers(),
				interceptor
		);
		DocumentBuilderIndexedEntity<T> documentBuilder = (DocumentBuilderIndexedEntity<T>) entityMapping.getDocumentBuilder();
		newMapping.setDocumentBuilderIndexedEntity( documentBuilder );
		return newMapping;
	}


}
