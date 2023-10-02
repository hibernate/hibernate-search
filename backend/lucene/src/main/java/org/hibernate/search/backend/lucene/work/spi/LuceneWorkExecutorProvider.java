/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface LuceneWorkExecutorProvider {

	SimpleScheduledExecutor writeExecutor(Context context);

	interface Context {
		/**
		 * @return A provider of thread pools.
		 */
		ThreadPoolProvider threadPoolProvider();

		/**
		 * Gives access to various configuration properties that might be useful during executor instantiation.
		 */
		ConfigurationPropertySource propertySource();

		/**
		 * @return recommended thread name prefix that can be passed to work executor. Recommendation is based on the
		 * instantiation context.
		 */
		String recommendedThreadNamePrefix();
	}

}
