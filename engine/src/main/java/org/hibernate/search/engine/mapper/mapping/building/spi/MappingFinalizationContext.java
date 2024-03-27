/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * The context passed to the mapper during the very last step of bootstrap.
 */
public interface MappingFinalizationContext {

	ContextualFailureCollector failureCollector();

	ConfigurationPropertySource configurationPropertySource();

	BeanResolver beanResolver();

}
