/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.FailureCollector;

class RootBuildContext {

	private final ConfigurationPropertySource propertySource;

	private final ClassResolver classResolver;
	private final ResourceResolver resourceResolver;
	private final BeanResolver beanResolver;

	private final FailureCollector failureCollector;
	private final ThreadPoolProvider threadPoolProvider;
	private final FailureHandler failureHandler;

	private final EngineThreads engineThreads;
	private final TimingSource timingSource;

	RootBuildContext(ConfigurationPropertySource propertySource,
			ClassResolver classResolver, ResourceResolver resourceResolver,
			BeanResolver beanResolver,
			FailureCollector failureCollector,
			ThreadPoolProvider threadPoolProvider, FailureHandler failureHandler,
			EngineThreads engineThreads, TimingSource timingSource) {
		this.propertySource = propertySource;
		this.classResolver = classResolver;
		this.resourceResolver = resourceResolver;
		this.beanResolver = beanResolver;
		this.failureCollector = failureCollector;
		this.threadPoolProvider = threadPoolProvider;
		this.failureHandler = failureHandler;
		this.engineThreads = engineThreads;
		this.timingSource = timingSource;
	}

	ConfigurationPropertySource getConfigurationPropertySource() {
		return propertySource;
	}

	ClassResolver getClassResolver() {
		return classResolver;
	}

	ResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	BeanResolver getBeanResolver() {
		return beanResolver;
	}

	FailureCollector getFailureCollector() {
		return failureCollector;
	}

	ThreadPoolProvider getThreadPoolProvider() {
		return threadPoolProvider;
	}

	FailureHandler getFailureHandler() {
		return failureHandler;
	}

	EngineThreads getEngineThreads() {
		return engineThreads;
	}

	TimingSource getTimingSource() {
		return timingSource;
	}
}
