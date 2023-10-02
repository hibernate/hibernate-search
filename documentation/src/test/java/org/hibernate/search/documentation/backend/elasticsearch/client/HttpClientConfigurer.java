/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.client;

import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

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
		clientBuilder.setMaxConnPerRoute( 7 ); // <4>
		clientBuilder.addInterceptorFirst( (HttpResponseInterceptor) (request, httpContext) -> {
			long contentLength = request.getEntity().getContentLength();
			// doing some stuff with contentLength
		} );
	}
}
// end::include[]
