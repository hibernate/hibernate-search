/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingStrategyStartContext;

public class AutomaticIndexingStrategyStartContextImpl implements AutomaticIndexingStrategyStartContext {
	private final MappingStartContext delegate;
	private final ConfigurationPropertySource configurationPropertySource;

	public AutomaticIndexingStrategyStartContextImpl(MappingStartContext delegate) {
		this.delegate = delegate;
		this.configurationPropertySource = delegate.configurationPropertySource();
	}

	@Override
	public BeanResolver beanResolver() {
		return delegate.beanResolver();
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return configurationPropertySource;
	}

}
