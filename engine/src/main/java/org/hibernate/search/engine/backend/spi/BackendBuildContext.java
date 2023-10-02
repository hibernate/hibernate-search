/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.spi;

import java.util.Optional;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;

/**
 * A build context for backends.
 */
public interface BackendBuildContext {

	ClassResolver classResolver();

	ResourceResolver resourceResolver();

	BeanResolver beanResolver();

	ThreadPoolProvider threadPoolProvider();

	FailureHandler failureHandler();

	TimingSource timingSource();

	boolean multiTenancyEnabled();

	Optional<String> backendName();

}
