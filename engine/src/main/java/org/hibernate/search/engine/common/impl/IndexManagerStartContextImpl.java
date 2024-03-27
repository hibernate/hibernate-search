/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class IndexManagerStartContextImpl implements IndexManagerStartContext {
	private final ContextualFailureCollector failureCollector;
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;

	IndexManagerStartContextImpl(ContextualFailureCollector failureCollector,
			BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource) {
		this.failureCollector = failureCollector;
		this.beanResolver = beanResolver;
		this.configurationPropertySource = configurationPropertySource;
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		return failureCollector;
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return configurationPropertySource;
	}
}
