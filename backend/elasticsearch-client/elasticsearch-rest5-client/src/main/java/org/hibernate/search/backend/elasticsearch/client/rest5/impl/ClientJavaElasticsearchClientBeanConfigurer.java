/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest5.impl;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

public class ClientJavaElasticsearchClientBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				ElasticsearchClientFactory.class, "elasticsearch-java",
				beanResolver -> BeanHolder.of( new ClientJavaElasticsearchClientFactory() )
		);
	}
}
