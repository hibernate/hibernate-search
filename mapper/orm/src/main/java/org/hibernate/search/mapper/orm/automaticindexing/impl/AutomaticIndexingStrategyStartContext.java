/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface AutomaticIndexingStrategyStartContext {

	BeanResolver beanResolver();

	ConfigurationPropertySource configurationPropertySource();

}
