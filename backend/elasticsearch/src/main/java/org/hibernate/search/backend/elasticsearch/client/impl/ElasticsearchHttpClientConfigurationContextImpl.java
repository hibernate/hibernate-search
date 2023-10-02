/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

final class ElasticsearchHttpClientConfigurationContextImpl
		implements ElasticsearchHttpClientConfigurationContext {
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;
	private final HttpAsyncClientBuilder clientBuilder;
	private final Optional<ElasticsearchVersion> configuredVersion;

	ElasticsearchHttpClientConfigurationContextImpl(
			BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource,
			HttpAsyncClientBuilder clientBuilder,
			Optional<ElasticsearchVersion> configuredVersion) {
		this.beanResolver = beanResolver;
		this.configurationPropertySource = configurationPropertySource;
		this.clientBuilder = clientBuilder;
		this.configuredVersion = configuredVersion;
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
	public HttpAsyncClientBuilder clientBuilder() {
		return clientBuilder;
	}

	@Override
	public Optional<ElasticsearchVersion> configuredVersion() {
		return configuredVersion;
	}

}
