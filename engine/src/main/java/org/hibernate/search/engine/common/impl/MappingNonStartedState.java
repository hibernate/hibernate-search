/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
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
			ConfigurationPropertySource propertySource) {
		ContextualFailureCollector mappingFailureCollector = rootFailureCollector.withContext( key );
		MappingStartContextImpl startContext = new MappingStartContextImpl(
				mappingFailureCollector,
				beanResolver,
				propertySource
		);
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
