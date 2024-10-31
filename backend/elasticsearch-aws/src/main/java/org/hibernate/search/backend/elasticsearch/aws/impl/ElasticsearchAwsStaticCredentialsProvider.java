/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsCredentialsTypeNames;
import org.hibernate.search.backend.elasticsearch.aws.logging.impl.AwsLog;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsCredentialsProvider;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class ElasticsearchAwsStaticCredentialsProvider implements ElasticsearchAwsCredentialsProvider {

	static final OptionalConfigurationProperty<String> CREDENTIALS_ACCESS_KEY_ID =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.CREDENTIALS_ACCESS_KEY_ID )
					.asString()
					.build();

	static final OptionalConfigurationProperty<String> CREDENTIALS_SECRET_ACCESS_KEY =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.CREDENTIALS_SECRET_ACCESS_KEY )
					.asString()
					.build();

	@Override
	public AwsCredentialsProvider create(ConfigurationPropertySource propertySource) {
		String accessKey = CREDENTIALS_ACCESS_KEY_ID.getOrThrow( propertySource,
				() -> AwsLog.INSTANCE.missingPropertyForSigningWithCredentialsType( ElasticsearchAwsCredentialsTypeNames.STATIC ) );
		String secretKey = CREDENTIALS_SECRET_ACCESS_KEY.getOrThrow( propertySource,
				() -> AwsLog.INSTANCE.missingPropertyForSigningWithCredentialsType( ElasticsearchAwsCredentialsTypeNames.STATIC ) );
		return StaticCredentialsProvider.create( AwsBasicCredentials.create( accessKey, secretKey ) );
	}
}
