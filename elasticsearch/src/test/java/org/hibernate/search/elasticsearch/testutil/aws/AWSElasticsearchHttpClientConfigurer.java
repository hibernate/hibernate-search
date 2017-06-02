/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil.aws;

import java.util.Properties;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.hibernate.search.elasticsearch.client.spi.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.util.StringHelper;

/**
 * @author Yoann Rodiere
 */
public class AWSElasticsearchHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

	private static final String ACCESS_KEY_PROPERTY = "elasticsearch.aws.access_key";
	private static final String SECRET_KEY_PROPERTY = "elasticsearch.aws.secret_key";
	private static final String REGION_PROPERTY = "elasticsearch.aws.region";
	private static final String ELASTICSEARCH_SERVICE_NAME = "es";

	@Override
	public void configure(HttpAsyncClientBuilder builder, Properties properties) {
		String accessKey = properties.getProperty( ACCESS_KEY_PROPERTY );
		String secretKey = properties.getProperty( SECRET_KEY_PROPERTY );
		String region = properties.getProperty( REGION_PROPERTY );

		if ( StringHelper.isEmpty( accessKey )
				&& StringHelper.isEmpty( secretKey )
				&& StringHelper.isEmpty( region ) ) {
			// AWS authentication isn't used
			return;
		}

		requireNonEmpty( accessKey, ACCESS_KEY_PROPERTY );
		requireNonEmpty( secretKey, SECRET_KEY_PROPERTY );
		requireNonEmpty( region, REGION_PROPERTY );

		AWSSigningRequestInterceptor interceptor = new AWSSigningRequestInterceptor(
				accessKey, secretKey, region,
				ELASTICSEARCH_SERVICE_NAME
				);

		builder.addInterceptorLast( interceptor );
	}

	private void requireNonEmpty(String value, String name) {
		if ( StringHelper.isEmpty( value ) ) {
			throw new IllegalStateException( "Missing value for property '" + name + "'." );
		}
	}

}
