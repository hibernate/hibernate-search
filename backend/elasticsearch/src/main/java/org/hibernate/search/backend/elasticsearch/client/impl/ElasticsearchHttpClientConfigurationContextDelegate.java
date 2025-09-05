/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

@SuppressWarnings("removal")
public class ElasticsearchHttpClientConfigurationContextDelegate implements ElasticsearchHttpClientConfigurationContext {

	private final org.hibernate.search.backend.elasticsearch.client.rest.ElasticsearchHttpClientConfigurationContext context;

	public ElasticsearchHttpClientConfigurationContextDelegate(
			org.hibernate.search.backend.elasticsearch.client.rest.ElasticsearchHttpClientConfigurationContext context) {
		this.context = context;
	}

	@Override
	public BeanResolver beanResolver() {
		return context.beanResolver();
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return context.configurationPropertySource();
	}

	@Override
	public HttpAsyncClientBuilder clientBuilder() {
		return context.clientBuilder();
	}
}
