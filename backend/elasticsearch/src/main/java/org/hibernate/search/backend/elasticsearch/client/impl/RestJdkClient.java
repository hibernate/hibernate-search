/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Closer;

@Incubating
public class RestJdkClient implements AutoCloseable {

	private final NodeProvider nodeProvider;

	private HttpClient httpClient;

	public RestJdkClient(NodeProvider nodeProvider, HttpClient httpClient) {
		this.nodeProvider = nodeProvider;
		this.httpClient = httpClient;
	}

	public NodeProvider.ServerNode nextNode() {
		return nodeProvider.nextNode();
	}

	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
			HttpResponse.BodyHandler<T> responseBodyHandler) {
		return httpClient.sendAsync( request, responseBodyHandler );
	}

	@Override
	public void close() throws Exception {
		if ( httpClient != null ) {
			try ( Closer<Exception> closer = new Closer<>() ) {
				Optional<Executor> executor = httpClient.executor();
				// May look a bit silly ... but close was only added in JDK 21:
				if ( ( (Object) httpClient ) instanceof AutoCloseable closeable ) {
					closer.push( AutoCloseable::close, closeable );
				}
				if ( executor.isPresent() && executor.get() instanceof AutoCloseable closeable ) {
					closer.push( AutoCloseable::close, closeable );
				}
			}
			finally {
				httpClient = null;
			}
		}
	}
}
