/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;

class DelegatingBuildContext {

	private final RootBuildContext delegate;

	DelegatingBuildContext(RootBuildContext delegate) {
		this.delegate = delegate;
	}

	public ClassResolver classResolver() {
		return delegate.getClassResolver();
	}

	public ResourceResolver resourceResolver() {
		return delegate.getResourceResolver();
	}

	public BeanResolver beanResolver() {
		return delegate.getBeanResolver();
	}

	public ConfigurationPropertySource configurationPropertySource() {
		return delegate.getConfigurationPropertySource();
	}

	public ThreadPoolProvider threadPoolProvider() {
		return delegate.getThreadPoolProvider();
	}

	public FailureHandler failureHandler() {
		return delegate.getFailureHandler();
	}

	public EngineThreads engineThreads() {
		return delegate.getEngineThreads();
	}

	public TimingSource timingSource() {
		return delegate.getTimingSource();
	}
}
