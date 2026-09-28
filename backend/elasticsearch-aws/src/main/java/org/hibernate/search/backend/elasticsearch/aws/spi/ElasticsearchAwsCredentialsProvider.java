package org.hibernate.search.backend.elasticsearch.aws.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public interface ElasticsearchAwsCredentialsProvider {

	AwsCredentialsProvider create(ConfigurationPropertySource propertySource);

}
