/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorContext;

import software.amazon.awssdk.http.ContentStreamProvider;

public class HttpEntityContentStreamProvider implements ContentStreamProvider, Closeable {
	private final ElasticsearchRequestInterceptorContext requestContext;
	private InputStream previousStream;

	public HttpEntityContentStreamProvider(ElasticsearchRequestInterceptorContext requestContext) {
		this.requestContext = requestContext;
	}

	public static HttpEntityContentStreamProvider create(ElasticsearchRequestInterceptorContext requestContext) {
		if ( requestContext.hasContent() ) {
			return new HttpEntityContentStreamProvider( requestContext );
		}
		return null;
	}

	@Override
	public InputStream newStream() {
		try {
			// Believe it or not, the AWS SDK expects us to close previous streams ourselves...
			close();
			InputStream newStream = requestContext.content();
			previousStream = newStream;
			return newStream;
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void close() throws IOException {
		if ( previousStream != null ) {
			previousStream.close();
			previousStream = null;
		}
	}
}
