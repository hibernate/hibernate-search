/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
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
	public ContextualFailureCollector getFailureCollector() {
		return failureCollector;
	}

	@Override
	public BeanResolver getBeanResolver() {
		return beanResolver;
	}

	@Override
	public ConfigurationPropertySource getConfigurationPropertySource() {
		return configurationPropertySource;
	}
}
