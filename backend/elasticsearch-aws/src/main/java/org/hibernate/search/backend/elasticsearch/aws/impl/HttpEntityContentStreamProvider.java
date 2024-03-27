/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.http.HttpEntity;
import software.amazon.awssdk.http.ContentStreamProvider;

public class HttpEntityContentStreamProvider implements ContentStreamProvider, Closeable {
	private final HttpEntity entity;
	private InputStream previousStream;

	public HttpEntityContentStreamProvider(HttpEntity entity) {
		this.entity = entity;
	}

	@Override
	public InputStream newStream() {
		try {
			// Believe it or not, the AWS SDK expects us to close previous streams ourselves...
			close();
			InputStream newStream = entity.getContent();
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
