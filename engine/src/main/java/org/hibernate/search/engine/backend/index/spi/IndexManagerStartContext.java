/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.index.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * A start context for index managers.
 */
public interface IndexManagerStartContext {

	/**
	 * A collector of (non-fatal) failures, allowing to notify Hibernate Search
	 * that something went wrong and bootstrap should be aborted at some point,
	 * while still continuing the bootstrap process for some time to collect other errors
	 * that could be relevant to users.
	 *
	 * @return A failure collector.
	 */
	ContextualFailureCollector failureCollector();

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	ConfigurationPropertySource configurationPropertySource();

}
