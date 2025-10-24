/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

public class ElasticsearchClientBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				ElasticsearchClientFactory.class, ElasticsearchClientFactory.DEFAULT_BEAN_NAME,
				beanResolver -> BeanHolder.of( new ClientRest4ElasticsearchClientFactory() )
		);
	}
}
