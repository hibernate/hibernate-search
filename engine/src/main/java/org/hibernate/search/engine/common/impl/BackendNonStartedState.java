/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.ConfigurationPropertySourceExtractor;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.reporting.EventContext;

class BackendNonStartedState {

	private final EventContext eventContext;
	private final ConfigurationPropertySourceExtractor propertySourceExtractor;
	private final BackendImplementor backend;

	BackendNonStartedState(EventContext eventContext, ConfigurationPropertySourceExtractor propertySourceExtractor,
			BackendImplementor backend) {
		this.eventContext = eventContext;
		this.propertySourceExtractor = propertySourceExtractor;
		this.backend = backend;
	}

	void closeOnFailure() {
		backend.stop();
	}

	BackendImplementor start(RootFailureCollector rootFailureCollector,
			BeanResolver beanResolver,
			ConfigurationPropertySource rootPropertySource,
			ThreadPoolProvider threadPoolProvider) {
		ContextualFailureCollector backendFailureCollector = rootFailureCollector.withContext( eventContext );
		ConfigurationPropertySource backendPropertySource = propertySourceExtractor.extract( beanResolver, rootPropertySource );
		BackendStartContextImpl startContext = new BackendStartContextImpl(
				backendFailureCollector,
				beanResolver,
				backendPropertySource,
				threadPoolProvider
		);
		try {
			backend.start( startContext );
		}
		catch (RuntimeException e) {
			backendFailureCollector.add( e );
		}
		return backend; // The backend is now started
	}
}
