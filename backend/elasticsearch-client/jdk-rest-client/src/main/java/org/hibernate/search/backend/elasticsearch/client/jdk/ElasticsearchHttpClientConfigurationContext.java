/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.jdk;


import java.net.http.HttpClient;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.annotation.Incubating;

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
	 * @return A JDK HTTP client builder, to set the configuration.
	 * @see HttpClient.Builder
	 */
	HttpClient.Builder clientBuilder();

}
