/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface ConfigurationPropertySourceExtractor {

	ConfigurationPropertySource extract(BeanResolver beanResolver, ConfigurationPropertySource parentSource);

}
