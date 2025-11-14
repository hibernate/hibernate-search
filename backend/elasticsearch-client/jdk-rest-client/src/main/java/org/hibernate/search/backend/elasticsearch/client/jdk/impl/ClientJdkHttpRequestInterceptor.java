/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.jdk.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorContext;


record ClientJdkHttpRequestInterceptor(ElasticsearchRequestInterceptor elasticsearchRequestInterceptor)
		implements HttpRequestInterceptor {

	@Override
	public void process(HttpRequest.Builder request, HttpRequest.BodyPublisher bodyPublisher,
			HttpRequestInterceptorContext context)
			throws IOException {
		elasticsearchRequestInterceptor.intercept(
				new ClientJavaRequestContext( request.copy().build(), request, bodyPublisher, context )
		);
	}

	private record ClientJavaRequestContext(HttpRequest request, HttpRequest.Builder original,
											HttpRequest.BodyPublisher bodyPublisher, HttpRequestInterceptorContext context)
			implements ElasticsearchRequestInterceptorContext {

		@Override
		public boolean hasContent() {
			return !ClientJdkGsonHttpEntity.isNoBodyPublisher( bodyPublisher );
		}

		@Override
		public InputStream content() {
			if ( bodyPublisher instanceof ClientJdkGsonHttpEntity publisher ) {
				return publisher.getContent();
			}
			return null;
		}

		@Override
		public String scheme() {
			return request.uri().getScheme();
		}

		@Override
		public String host() {
			return request.uri().getHost();
		}

		@Override
		public Integer port() {
			return request.uri().getPort();
		}

		@Override
		public String method() {
			return context().method();
		}

		@Override
		public String path() {
			return request.uri().getPath();
		}

		@Override
		public Map<String, String> queryParameters() {
			String query = request.uri().getQuery();
			if ( query == null || query.isEmpty() ) {
				return Map.of();
			}

			Map<String, String> map = new HashMap<>();

			String[] params = query.split( "&" );

			for ( String param : params ) {
				String[] pair = param.split( "=", 2 );

				if ( pair.length == 2 ) {
					map.put(
							URLDecoder.decode( pair[0], StandardCharsets.UTF_8 ),
							URLDecoder.decode( pair[1], StandardCharsets.UTF_8 )
					);
				}
				else {
					map.put( URLDecoder.decode( pair[0], StandardCharsets.UTF_8 ), "" );
				}
			}

			return map;
		}

		@Override
		public void overrideHeaders(Map<String, List<String>> headers) {
			for ( Map.Entry<String, List<String>> header : headers.entrySet() ) {
				String name = header.getKey();
				boolean first = true;
				for ( String value : header.getValue() ) {
					if ( first ) {
						original.setHeader( name, value );
						first = false;
					}
					else {
						original.header( name, value );
					}
				}
			}
		}

		@Override
		public String toString() {
			return request.toString();
		}
	}
}
