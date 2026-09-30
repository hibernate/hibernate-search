package org.hibernate.search.backend.elasticsearch.client.rest4.impl;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProviderContext;
import org.hibernate.search.backend.elasticsearch.client.rest4.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

final class ClientRest4ElasticsearchHttpClientConfigurationContext
		implements ElasticsearchHttpClientConfigurationContext, ElasticsearchRequestInterceptorProviderContext {
	private final BeanResolver beanResolver;
	private final ConfigurationPropertySource configurationPropertySource;
	private final HttpAsyncClientBuilder clientBuilder;

	ClientRest4ElasticsearchHttpClientConfigurationContext(
			BeanResolver beanResolver,
			ConfigurationPropertySource configurationPropertySource,
			HttpAsyncClientBuilder clientBuilder) {
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
	public HttpAsyncClientBuilder clientBuilder() {
		return clientBuilder;
	}

}
