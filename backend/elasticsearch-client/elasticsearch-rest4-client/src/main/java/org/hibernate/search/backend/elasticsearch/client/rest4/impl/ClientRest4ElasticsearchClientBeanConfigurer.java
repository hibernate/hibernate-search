package org.hibernate.search.backend.elasticsearch.client.rest4.impl;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

public class ClientRest4ElasticsearchClientBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				ElasticsearchClientFactory.class, ClientRest4ElasticsearchClientFactory.NAME,
				beanResolver -> BeanHolder.of( new ClientRest4ElasticsearchClientFactory() )
		);
	}
}
