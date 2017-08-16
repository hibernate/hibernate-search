/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.aws.impl;

import java.util.Properties;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.hibernate.search.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;

/**
 * @author Yoann Rodiere
 */
public class AWSElasticsearchHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

	private static final String SIGNING_ENABLED_PROPERTY = "elasticsearch.aws.signing.enabled";
	private static final String ACCESS_KEY_PROPERTY = "elasticsearch.aws.access_key";
	private static final String SECRET_KEY_PROPERTY = "elasticsearch.aws.secret_key";
	private static final String REGION_PROPERTY = "elasticsearch.aws.region";
	private static final String ELASTICSEARCH_SERVICE_NAME = "es";

	@Override
	public void configure(HttpAsyncClientBuilder builder, Properties properties) {
		Boolean enabled = ConfigurationParseHelper.getBooleanValue(
				properties, SIGNING_ENABLED_PROPERTY, false );
		if ( !enabled ) {
			return;
		}

		String accessKey = requireNonEmpty( properties, ACCESS_KEY_PROPERTY );
		String secretKey = requireNonEmpty( properties, SECRET_KEY_PROPERTY );
		String region = requireNonEmpty( properties, REGION_PROPERTY );

		AWSPayloadHashingRequestInterceptor payloadHashingInterceptor =
				new AWSPayloadHashingRequestInterceptor();
		AWSSigningRequestInterceptor signingInterceptor = new AWSSigningRequestInterceptor(
				accessKey, secretKey, region,
				ELASTICSEARCH_SERVICE_NAME
				);

		builder.addInterceptorFirst( payloadHashingInterceptor );
		builder.addInterceptorLast( signingInterceptor );
	}

	private String requireNonEmpty(Properties properties, String name) {
		String value = properties.getProperty( name );
		if ( StringHelper.isEmpty( value ) ) {
			throw new SearchException( "Missing value for property '" + name + "'." );
		}
		return value;
	}

}
