/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.opensearch.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorContext;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;

record ClientOpenSearchHttpRequestInterceptor(ElasticsearchRequestInterceptor elasticsearchRequestInterceptor)
		implements HttpRequestInterceptor {

	@Override
	public void process(HttpRequest request, EntityDetails entity, HttpContext context) throws IOException {
		elasticsearchRequestInterceptor.intercept(
				new ClientOpenSearchRequestContext( request, entity, context )
		);
	}

	private record ClientOpenSearchRequestContext(HttpRequest request, EntityDetails entity, HttpClientContext clientContext)
			implements ElasticsearchRequestInterceptorContext {
		private ClientOpenSearchRequestContext(HttpRequest request, EntityDetails entity, HttpContext context) {
			this( request, entity, HttpClientContext.cast( context ) );
		}

		@Override
		public boolean hasContent() {
			return entity != null;
		}

		@Override
		public InputStream content() throws IOException {
			HttpEntity localEntity = null;
			if ( entity instanceof HttpEntity httpEntity ) {
				localEntity = httpEntity;
			}
			else if ( request instanceof HttpEntityContainer entityContainer ) {
				localEntity = entityContainer.getEntity();
			}

			if ( localEntity != null ) {
				if ( !localEntity.isRepeatable() ) {
					throw new AssertionFailure( "Cannot sign AWS requests with non-repeatable entities" );
				}
				return localEntity.getContent();
			}

			if ( entity instanceof AsyncEntityProducer producer ) {
				if ( !producer.isRepeatable() ) {
					throw new AssertionFailure( "Cannot sign AWS requests with non-repeatable entities" );
				}
				return new HttpAsyncEntityProducerInputStream( producer, 1024 );
			}
			return null;
		}

		@Override
		public String scheme() {
			return clientContext.getHttpRoute().getTargetHost().getSchemeName();
		}

		@Override
		public String host() {
			return clientContext.getHttpRoute().getTargetHost().getHostName();
		}

		@Override
		public Integer port() {
			return clientContext.getHttpRoute().getTargetHost().getPort();
		}

		@Override
		public String method() {
			return request.getMethod();
		}

		@Override
		public String path() {
			try {
				return request.getUri().getPath();
			}
			catch (URISyntaxException e) {
				return request.getPath();
			}
		}

		@Override
		public Map<String, String> queryParameters() {
			try {
				List<NameValuePair> queryParameters = new URIBuilder( request.getUri() ).getQueryParams();
				Map<String, String> map = new HashMap<>();
				for ( NameValuePair parameter : queryParameters ) {
					map.put( parameter.getName(), parameter.getValue() );
				}
				return map;
			}
			catch (URISyntaxException e) {
				return Map.of();
			}
		}

		@Override
		public void overrideHeaders(Map<String, List<String>> headers) {
			for ( Map.Entry<String, List<String>> header : headers.entrySet() ) {
				String name = header.getKey();
				boolean first = true;
				for ( String value : header.getValue() ) {
					if ( first ) {
						request.setHeader( name, value );
						first = false;
					}
					else {
						request.addHeader( name, value );
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
