/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizationContext;

class MappingFinalizationContextImpl implements MappingFinalizationContext {

	private final ConfigurationPropertySource propertySource;
	private final BeanResolver beanResolver;

	MappingFinalizationContextImpl(ConfigurationPropertySource propertySource, BeanResolver beanResolver) {
		this.propertySource = propertySource;
		this.beanResolver = beanResolver;
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
