/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.opensearch.rest;


import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.annotation.Incubating;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;


/**
 * The context passed to {@link ElasticsearchHttpClientConfigurer}.
 */
@Incubating
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
