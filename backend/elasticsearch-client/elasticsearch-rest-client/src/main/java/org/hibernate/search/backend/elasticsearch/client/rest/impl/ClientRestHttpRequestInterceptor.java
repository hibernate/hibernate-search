/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorContext;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

record ClientRestHttpRequestInterceptor(ElasticsearchRequestInterceptor elasticsearchRequestInterceptor)
		implements HttpRequestInterceptor {

	@Override
	public void process(HttpRequest request, HttpContext context) throws IOException {
		elasticsearchRequestInterceptor.intercept(
				new ClientRestRequestContext( request, context )
		);
	}

	private record ClientRestRequestContext(HttpRequest request, HttpCoreContext coreContext)
			implements ElasticsearchRequestInterceptorContext {
		private ClientRestRequestContext(HttpRequest request, HttpContext coreContext) {
			this( request, HttpCoreContext.adapt( coreContext ) );
		}

		@Override
		public boolean hasContent() {
			if ( request instanceof HttpEntityEnclosingRequest enclosingRequest ) {
				return enclosingRequest.getEntity() != null;
			}
			return false;
		}

		@Override
		public InputStream content() throws IOException {
			if ( request instanceof HttpEntityEnclosingRequest enclosingRequest ) {
				HttpEntity entity = enclosingRequest.getEntity();
				if ( entity == null ) {
					return null;
				}
				if ( !entity.isRepeatable() ) {
					throw new AssertionFailure( "Cannot sign AWS requests with non-repeatable entities" );
				}
				return entity.getContent();
			}
			else {
				return null;
			}
		}

		@Override
		public String scheme() {
			return coreContext.getTargetHost().getSchemeName();
		}

		@Override
		public String host() {
			return coreContext.getTargetHost().getHostName();
		}

		@Override
		public Integer port() {
			return coreContext.getTargetHost().getPort();
		}

		@Override
		public String method() {
			return request.getRequestLine().getMethod();
		}

		@Override
		public String path() {
			String uri = request.getRequestLine().getUri();
			int queryStart = uri.indexOf( '?' );
			if ( queryStart >= 0 ) {
				return uri.substring( 0, queryStart );
			}
			return uri;
		}

		@Override
		public Map<String, String> queryParameters() {
			String pathAndQuery = request.getRequestLine().getUri();
			List<NameValuePair> queryParameters;
			int queryStart = pathAndQuery.indexOf( '?' );
			if ( queryStart >= 0 ) {
				queryParameters = URLEncodedUtils.parse( pathAndQuery.substring( queryStart + 1 ), StandardCharsets.UTF_8 );
				Map<String, String> map = new HashMap<>();
				for ( NameValuePair parameter : queryParameters ) {
					map.put( parameter.getName(), parameter.getValue() );
				}
				return map;
			}
			return Map.of();
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
