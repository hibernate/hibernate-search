/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizationContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class MappingFinalizationContextImpl implements MappingFinalizationContext {

	private final ContextualFailureCollector failureCollector;
	private final ConfigurationPropertySource propertySource;
	private final BeanResolver beanResolver;

	MappingFinalizationContextImpl(ContextualFailureCollector failureCollector,
			ConfigurationPropertySource propertySource, BeanResolver beanResolver) {
		this.failureCollector = failureCollector;
		this.propertySource = propertySource;
		this.beanResolver = beanResolver;
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		return failureCollector;
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return propertySource;
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}
}
