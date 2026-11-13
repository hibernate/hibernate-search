package org.hibernate.search.backend.elasticsearch.aws.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public interface ElasticsearchAwsCredentialsProvider {

	/**
	 * Create credentials for the configured integration. The asynchronous preparation provider owns
	 * the returned provider and closes it at client shutdown if it implements AutoCloseable.
	 * Return a non-owning wrapper for externally managed/shared providers. The legacy synchronous
	 * interceptor lifecycle is unchanged.
	 */
	AwsCredentialsProvider create(ConfigurationPropertySource propertySource);

}
