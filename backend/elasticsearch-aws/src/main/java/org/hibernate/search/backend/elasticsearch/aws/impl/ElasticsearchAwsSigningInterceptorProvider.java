/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsCredentialsTypeNames;
import org.hibernate.search.backend.elasticsearch.aws.logging.impl.AwsLog;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsCredentialsProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProviderContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class ElasticsearchAwsSigningInterceptorProvider implements ElasticsearchRequestInterceptorProvider {
	private static final Pattern DISTRIBUTION_NAME_PATTERN = Pattern.compile( "([^\\d]+)?(?:(?<=^)|(?=$)|(?<=.):(?=.))(.+)?" );
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

	static final OptionalConfigurationProperty<String> DISTRIBUTION_NAME =
			ConfigurationProperty.forKey( "version" )
					.asString()
					.build();

	@Override
	public Optional<ElasticsearchRequestInterceptor> provide(ElasticsearchRequestInterceptorProviderContext context) {
		ConfigurationPropertySource propertySource = context.configurationPropertySource();

		if ( !SIGNING_ENABLED.get( propertySource ) ) {
			AwsLog.INSTANCE.signingDisabled();
			return Optional.empty();
		}

		Region region = REGION.getAndMapOrThrow( propertySource, Region::of, AwsLog.INSTANCE::missingPropertyForSigning );
		String service;

		String distributionName = DISTRIBUTION_NAME.getAndTransform( propertySource,
				v -> v.map( ver -> ver.toLowerCase( Locale.ROOT ) )
						.map( DISTRIBUTION_NAME_PATTERN::matcher )
						.map( matcher -> {
							if ( matcher.matches() ) {
								return matcher.group( 1 );
							}
							return null;
						} ).orElse( "opensearch" ) );

		if ( "amazon-opensearch-serverless".equals( distributionName ) ) {
			service = "aoss";
		}
		else {
			service = "es";
		}

		AwsCredentialsProvider credentialsProvider = createCredentialsProvider( context.beanResolver(), propertySource );

		AwsLog.INSTANCE.signingEnabled( region, service, credentialsProvider );

		ElasticsearchAwsSigningRequestInterceptor signingInterceptor =
				new ElasticsearchAwsSigningRequestInterceptor( region, service, credentialsProvider );

		return Optional.of( signingInterceptor );
	}

	private AwsCredentialsProvider createCredentialsProvider(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		if ( LEGACY_ACCESS_KEY.get( propertySource ).isPresent()
				|| LEGACY_SECRET_KEY.get( propertySource ).isPresent() ) {
			throw AwsLog.INSTANCE.obsoleteAccessKeyIdOrSecretAccessKeyForSigning(
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
