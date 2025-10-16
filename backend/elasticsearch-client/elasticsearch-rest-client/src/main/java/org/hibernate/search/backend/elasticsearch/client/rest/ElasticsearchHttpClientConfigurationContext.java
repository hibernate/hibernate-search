/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest;


import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * The context passed to {@link ElasticsearchHttpClientConfigurer}.
 */
@SuppressJQAssistant(
		reason = "Apache HTTP Client 5 uses a lot of classes/interfaces in the impl packages to create builders/instances etc. "
				+ "So while it is bad to expose impl types ... in this case it's what Apache Client expects users to do?")
public interface ElasticsearchHttpClientConfigurationContext {

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	/**
	 * @return A configuration property source, appropriately masked so that the factory
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key "type" is reserved for use by the engine.
	 */
	ConfigurationPropertySource configurationPropertySource();

	/**
	 * @return An Apache HTTP client builder, to set the configuration.
	 * @see <a href="http://hc.apache.org/httpcomponents-client-ga/">the Apache HTTP Client documentation</a>
	 */
	HttpAsyncClientBuilder clientBuilder();

}
