/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsCredentialsTypeNames;
import org.hibernate.search.backend.elasticsearch.aws.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsCredentialsProvider;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class ElasticsearchAwsHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> SIGNING_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.SIGNING_ENABLED )
					.asBoolean()
					.withDefault( ElasticsearchAwsBackendSettings.Defaults.SIGNING_ENABLED )
					.build();

	private static final OptionalConfigurationProperty<String> REGION =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.REGION )
					.asString()
					.build();

	private static final ConfigurationProperty<BeanReference<? extends ElasticsearchAwsCredentialsProvider>> CREDENTIALS_TYPE =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.CREDENTIALS_TYPE )
					.asBeanReference( ElasticsearchAwsCredentialsProvider.class )
					.withDefault( BeanReference.of( ElasticsearchAwsCredentialsProvider.class,
							ElasticsearchAwsBackendSettings.Defaults.CREDENTIALS_TYPE ) )
					.build();

	private static final OptionalConfigurationProperty<String> LEGACY_ACCESS_KEY =
			ConfigurationProperty.forKey( "aws.signing.access_key" )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> LEGACY_SECRET_KEY =
			ConfigurationProperty.forKey( "aws.signing.secret_key" )
					.asString()
					.build();

	@Override
	public void configure(ElasticsearchHttpClientConfigurationContext context) {
		ConfigurationPropertySource propertySource = context.configurationPropertySource();

		if ( !SIGNING_ENABLED.get( propertySource ) ) {
			log.debug( "AWS request signing is disabled." );
			return;
		}

		Region region = REGION.getAndMapOrThrow( propertySource, Region::of, log::missingPropertyForSigning );
		String service;
		switch ( context.configuredVersion().map( ElasticsearchVersion::distribution )
				.orElse( ElasticsearchDistributionName.OPENSEARCH ) ) {
			case AMAZON_OPENSEARCH_SERVERLESS:
				service = "aoss";
				break;
			case ELASTIC:
			case OPENSEARCH:
			default:
				service = "es";
				break;
		}
		AwsCredentialsProvider credentialsProvider = createCredentialsProvider( context.beanResolver(), propertySource );

		log.debugf( "AWS request signing is enabled [region = '%s', service = '%s', credentialsProvider = '%s'].",
				region, service, credentialsProvider );

		AwsSigningRequestInterceptor signingInterceptor =
				new AwsSigningRequestInterceptor( region, service, credentialsProvider );

		context.clientBuilder().addInterceptorLast( signingInterceptor );
	}

	private AwsCredentialsProvider createCredentialsProvider(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		if ( LEGACY_ACCESS_KEY.get( propertySource ).isPresent()
				|| LEGACY_SECRET_KEY.get( propertySource ).isPresent() ) {
			throw log.obsoleteAccessKeyIdOrSecretAccessKeyForSigning(
					LEGACY_ACCESS_KEY.resolveOrRaw( propertySource ),
					LEGACY_SECRET_KEY.resolveOrRaw( propertySource ),
					CREDENTIALS_TYPE.resolveOrRaw( propertySource ),
					ElasticsearchAwsCredentialsTypeNames.STATIC,
					ElasticsearchAwsStaticCredentialsProvider.CREDENTIALS_ACCESS_KEY_ID.resolveOrRaw( propertySource ),
					ElasticsearchAwsStaticCredentialsProvider.CREDENTIALS_SECRET_ACCESS_KEY.resolveOrRaw( propertySource ) );
		}

		try ( BeanHolder<? extends ElasticsearchAwsCredentialsProvider> credentialsProviderHolder =
				CREDENTIALS_TYPE.getAndTransform( propertySource, beanResolver::resolve ) ) {
			return credentialsProviderHolder.get().create( propertySource );
		}
	}

}
