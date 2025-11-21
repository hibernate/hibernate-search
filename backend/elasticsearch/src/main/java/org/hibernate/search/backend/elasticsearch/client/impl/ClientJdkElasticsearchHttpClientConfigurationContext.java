/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.net.http.HttpClient;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProviderContext;
import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

final class ClientJdkElasticsearchHttpClientConfigurationContext
		implements ElasticsearchHttpClientConfigurationContext, ElasticsearchRequestInterceptorProviderContext {
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;
	private final HttpClient.Builder clientBuilder;

	ClientJdkElasticsearchHttpClientConfigurationContext(
			BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource,
			HttpClient.Builder clientBuilder) {
		this.beanResolver = beanResolver;
		this.configurationPropertySource = configurationPropertySource;
		this.clientBuilder = clientBuilder;
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}

	@Override
	public ConfigurationPropertySource configurationPropertySource() {
		return configurationPropertySource;
	}

	@Override
	public HttpClient.Builder clientBuilder() {
		return clientBuilder;
	}

}
