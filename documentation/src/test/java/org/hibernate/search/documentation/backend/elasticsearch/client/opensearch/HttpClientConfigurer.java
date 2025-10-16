/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.client.opensearch;

import org.hibernate.search.backend.elasticsearch.client.opensearch.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.opensearch.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;

// tag::include[]
public class HttpClientConfigurer implements ElasticsearchHttpClientConfigurer { // <1>
	// end::include[]
	static final StaticCounters.Key INSTANCES = StaticCounters.createKey();

	public HttpClientConfigurer() {
		StaticCounters.get().increment( INSTANCES );
	}

	@SuppressWarnings("unused")
	// tag::include[]

	@Override
	public void configure(ElasticsearchHttpClientConfigurationContext context) { // <2>
		HttpAsyncClientBuilder clientBuilder = context.clientBuilder(); // <3>
		clientBuilder.setConnectionManager( // <4>
				PoolingAsyncClientConnectionManagerBuilder.create()
						.setMaxConnTotal( 20 )
						.setMaxConnPerRoute( 10 )
						.build()
		);
		clientBuilder.addResponseInterceptorFirst( (response, entityDetails, httpContext) -> {
			long contentLength = entityDetails.getContentLength();
			// doing some stuff with contentLength
		} );
	}
}
// end::include[]
