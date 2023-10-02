/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.resources.impl;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.work.spi.ElasticsearchWorkExecutorProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;

public class DefaultElasticsearchWorkExecutorProvider implements ElasticsearchWorkExecutorProvider {

	public static final String DEFAULT_BEAN_NAME = "es-built-in";

	private static final OptionalConfigurationProperty<Integer> THREAD_POOL_SIZE =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.THREAD_POOL_SIZE )
					.asIntegerStrictlyPositive()
					.build();

	@Override
	public SimpleScheduledExecutor workExecutor(ElasticsearchWorkExecutorProvider.Context context) {
		int threadPoolSize = THREAD_POOL_SIZE.get( context.propertySource() )
				.orElse( Runtime.getRuntime().availableProcessors() );
		// We use a scheduled executor so that we can also schedule client timeouts in the same thread pool.
		return new DelegatingSimpleScheduledExecutor(
				context.threadPoolProvider().newScheduledExecutor(
						threadPoolSize,
						context.recommendedThreadNamePrefix()
				),
				context.threadPoolProvider().isScheduledExecutorBlocking()
		);
	}
}
