/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil.aws;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.hibernate.search.elasticsearch.client.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.util.StringHelper;

/**
 * @author Yoann Rodiere
 */
public class AWSElasticsearchHttpClientConfigurer implements ElasticsearchHttpClientConfigurer {

	private static final String ELASTICSEARCH_SERVICE_NAME = "es";

	@Override
	public HttpAsyncClientBuilder configure(HttpAsyncClientBuilder builder) {
		AWSSigningRequestInterceptor interceptor = new AWSSigningRequestInterceptor(
				getEnv( "AWS_ACCESS_KEY_ID" ),
				getEnv( "AWS_SECRET_KEY" ),
				getEnv( "AWS_REGION" ),
				ELASTICSEARCH_SERVICE_NAME
				);

		return builder.addInterceptorLast( interceptor );
	}

	private String getEnv(String name) {
		String value = System.getenv( name );
		if ( StringHelper.isEmpty( value ) ) {
			throw new IllegalStateException( "Missing value for environment variable '" + name + "'." );
		}
		return value;
	}

}
