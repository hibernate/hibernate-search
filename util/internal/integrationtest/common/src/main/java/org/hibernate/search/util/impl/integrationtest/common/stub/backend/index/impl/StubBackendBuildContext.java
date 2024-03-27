/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;

public class StubBackendBuildContext implements BackendBuildContext {

	private final BackendBuildContext delegate;
	private final ConfigurationPropertySource backendConfigurationPropertySource;

	public StubBackendBuildContext(BackendBuildContext delegate,
			ConfigurationPropertySource backendConfigurationPropertySource) {
		this.delegate = delegate;
		this.backendConfigurationPropertySource = backendConfigurationPropertySource;
	}

	@Override
	public ClassResolver classResolver() {
		return delegate.classResolver();
	}

	@Override
	public ResourceResolver resourceResolver() {
		return delegate.resourceResolver();
	}

	@Override
	public BeanResolver beanResolver() {
		return delegate.beanResolver();
	}

	@Override
	public ThreadPoolProvider threadPoolProvider() {
		return delegate.threadPoolProvider();
	}

	@Override
	public FailureHandler failureHandler() {
		return delegate.failureHandler();
	}

	@Override
	public TimingSource timingSource() {
		return delegate.timingSource();
	}

	@Override
	public boolean multiTenancyEnabled() {
		return delegate.multiTenancyEnabled();
	}

	@Override
	public Optional<String> backendName() {
		return delegate.backendName();
	}

	public ConfigurationPropertySource backendConfigurationPropertySource() {
		return backendConfigurationPropertySource;
	}
}
