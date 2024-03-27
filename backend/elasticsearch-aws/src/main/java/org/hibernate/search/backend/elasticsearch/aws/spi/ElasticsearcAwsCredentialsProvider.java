/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * @deprecated Implement {@link ElasticsearchAwsCredentialsProvider} instead.
 */
@Deprecated
public interface ElasticsearcAwsCredentialsProvider extends ElasticsearchAwsCredentialsProvider {

	AwsCredentialsProvider create(ConfigurationPropertySource propertySource);

}
