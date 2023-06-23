/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class BackendStartContextImpl implements BackendStartContext {
	private final ContextualFailureCollector failureCollector;
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;
	private final ThreadPoolProvider threadPoolProvider;

	BackendStartContextImpl(ContextualFailureCollector failureCollector,
			BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource,
			ThreadPoolProvider threadPoolProvider) {
		this.failureCollector = failureCollector;
		this.beanResolver = beanResolver;
		this.configurationPropertySource = configurationPropertySource;
		this.threadPoolProvider = threadPoolProvider;
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

	@Override
	public ThreadPoolProvider threadPoolProvider() {
		return threadPoolProvider;
	}
}
