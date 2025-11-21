/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.common.spi;

import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Creates the Elasticsearch client.
 */
public interface ElasticsearchClientFactory {

	@Incubating
	String DEFAULT_BEAN_NAME = "default";

	@Incubating
	String SIMPLE_JDK_CLIENT_BEAN_NAME = "jdk-rest-client";

	ElasticsearchClientImplementor create(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			SimpleScheduledExecutor timeoutExecutorService,
			GsonProvider gsonProvider);

}
