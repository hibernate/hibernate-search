/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsCredentialsTypeNames;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearcAwsCredentialsProvider;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
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
				ElasticsearcAwsCredentialsProvider.class, ElasticsearchAwsCredentialsTypeNames.DEFAULT,
				beanResolver -> BeanHolder.of( ignored -> DefaultCredentialsProvider.create() )
		);
		context.define(
				ElasticsearcAwsCredentialsProvider.class, ElasticsearchAwsCredentialsTypeNames.STATIC,
				beanResolver -> BeanHolder.of( new ElasticsearchAwsStaticCredentialsProvider() )
		);
	}
}
