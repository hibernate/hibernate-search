/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.client.jdk;


import java.net.http.HttpClient;
import java.time.Duration;

import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurationContext;
import org.hibernate.search.backend.elasticsearch.client.jdk.ElasticsearchHttpClientConfigurer;
import org.hibernate.search.util.impl.test.extension.StaticCounters;


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
		HttpClient.Builder builder = context.clientBuilder(); // <3>
		builder.connectTimeout( Duration.ofSeconds( 2 ) ); // <4>
	}
}
// end::include[]
