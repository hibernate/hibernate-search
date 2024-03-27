/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

class MappingNonStartedState {

	private final MappingKey<?, ?> key;
	private final MappingImplementor<?> mapping;

	MappingNonStartedState(MappingKey<?, ?> key, MappingImplementor<?> mapping) {
		this.key = key;
		this.mapping = mapping;
	}

	void closeOnFailure() {
		mapping.stop();
	}

	CompletableFuture<?> start(RootFailureCollector rootFailureCollector, BeanResolver beanResolver,
			ConfigurationPropertySource propertySource, ThreadPoolProvider threadPoolProvider,
			SearchIntegration.Handle integrationHandle) {
		ContextualFailureCollector mappingFailureCollector = rootFailureCollector.withContext( key );
		MappingStartContextImpl startContext = new MappingStartContextImpl( mappingFailureCollector, beanResolver,
				propertySource, threadPoolProvider, integrationHandle );
		return mapping.start( startContext )
				.exceptionally( Futures.handler( e -> {
					mappingFailureCollector.add( Throwables.expectException( e ) );
					return null;
				} ) );
	}

	MappingImplementor<?> getMapping() {
		return mapping;
	}
}
