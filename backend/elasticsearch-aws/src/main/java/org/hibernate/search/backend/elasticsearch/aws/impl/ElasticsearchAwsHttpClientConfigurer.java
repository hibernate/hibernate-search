/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.aws.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchAwsHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> SIGNING_ENABLED =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.SIGNING_ENABLED )
					.asBoolean()
					.withDefault( ElasticsearchAwsBackendSettings.Defaults.SIGNING_ENABLED )
					.build();

	private static final OptionalConfigurationProperty<String> ACCESS_KEY =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.SIGNING_ACCESS_KEY )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> SECRET_KEY =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.SIGNING_SECRET_KEY )
					.asString()
					.build();

	private static final OptionalConfigurationProperty<String> REGION =
			ConfigurationProperty.forKey( ElasticsearchAwsBackendSettings.SIGNING_REGION )
					.asString()
					.build();

	@Override
	public void configure(ElasticsearchHttpClientConfigurationContext context) {
		ConfigurationPropertySource propertySource = context.configurationPropertySource();

		if ( !SIGNING_ENABLED.get( propertySource ) ) {
			return;
		}

		Region region = REGION.getAndMapOrThrow( propertySource, Region::of, log::missingPropertyForSigning );

		AwsCredentialsProvider credentialsProvider = createCredentialsProvider( propertySource );

		AwsSigningRequestInterceptor signingInterceptor = new AwsSigningRequestInterceptor( region, credentialsProvider );

		context.clientBuilder().addInterceptorLast( signingInterceptor );
	}

	private AwsCredentialsProvider createCredentialsProvider(ConfigurationPropertySource propertySource) {
		Optional<String> accessKeyOptional = ACCESS_KEY.get( propertySource );
		Optional<String> secretKeyOptional = SECRET_KEY.get( propertySource );
		if ( accessKeyOptional.isPresent() || secretKeyOptional.isPresent() ) {
			if ( !accessKeyOptional.isPresent() ) {
				throw log.missingAccessKeyForSigningWithSecretKeySet( ACCESS_KEY.resolveOrRaw( propertySource ) );
			}
			if ( !secretKeyOptional.isPresent() ) {
				throw log.missingSecretKeyForSigningWithAccessKeySet( SECRET_KEY.resolveOrRaw( propertySource ) );
			}
			return StaticCredentialsProvider.create(
					AwsBasicCredentials.create( accessKeyOptional.get(), secretKeyOptional.get() )
			);
		}
		else {
			return DefaultCredentialsProvider.create();
		}
	}

}
