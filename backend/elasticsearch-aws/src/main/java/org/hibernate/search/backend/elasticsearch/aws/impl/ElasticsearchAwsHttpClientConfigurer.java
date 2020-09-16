/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.util.function.Function;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.regions.Region;

import org.hibernate.search.backend.elasticsearch.aws.cfg.ElasticsearchAwsBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;

public class ElasticsearchAwsHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

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

	private static final String ELASTICSEARCH_SERVICE_NAME = "es";

	@Override
	public void configure(HttpAsyncClientBuilder builder, ConfigurationPropertySource propertySource) {
		if ( !SIGNING_ENABLED.get( propertySource ) ) {
			return;
		}

		String accessKey = getMandatory( ACCESS_KEY, propertySource, Function.identity() );
		String secretKey = getMandatory( SECRET_KEY, propertySource, Function.identity() );
		Region region = getMandatory( REGION, propertySource, Region::of );
		Aws4SignerParams signerParams = Aws4SignerParams.builder()
				.awsCredentials( AwsBasicCredentials.create( accessKey, secretKey ) )
				.signingRegion( region )
				.signingName( ELASTICSEARCH_SERVICE_NAME )
				.build();

		AwsSigningRequestInterceptor signingInterceptor = new AwsSigningRequestInterceptor( signerParams );

		builder.addInterceptorLast( signingInterceptor );
	}

	private <T, R> R getMandatory(OptionalConfigurationProperty<T> property, ConfigurationPropertySource propertySource,
			Function<T, R> transform) {
		return property.getAndMapOrThrow(
				propertySource,
				transform,
				key -> new IllegalStateException(
						"AWS request signing is enabled, but mandatory property '" + key + "' is not set"
				)
		);
	}

}
