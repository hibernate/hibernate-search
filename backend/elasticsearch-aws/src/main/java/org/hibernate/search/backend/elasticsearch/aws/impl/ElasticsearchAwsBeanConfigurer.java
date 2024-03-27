/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsCredentialsTypeNames;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsCredentialsProvider;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class ElasticsearchAwsBeanConfigurer implements BeanConfigurer {
	@Override
	public void configure(BeanConfigurationContext context) {
		context.define(
				ElasticsearchHttpClientConfigurer.class,
				beanResolver -> BeanHolder.of( new ElasticsearchAwsHttpClientConfigurer() )
		);
		context.define(
				ElasticsearchAwsCredentialsProvider.class, ElasticsearchAwsCredentialsTypeNames.DEFAULT,
				beanResolver -> BeanHolder.of( ignored -> DefaultCredentialsProvider.create() )
		);
		context.define(
				ElasticsearchAwsCredentialsProvider.class, ElasticsearchAwsCredentialsTypeNames.STATIC,
				beanResolver -> BeanHolder.of( new ElasticsearchAwsStaticCredentialsProvider() )
		);
	}
}
