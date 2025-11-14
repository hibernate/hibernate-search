/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.jdk.cfg.spi;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public class RestJdkClient implements AutoCloseable {

	private final NodeProvider nodeProvider;

	private final HttpClient httpClient;

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
		// May look a bit silly ... but close was only added in JDK 21:
		if ( httpClient instanceof AutoCloseable closeable ) {
			closeable.close();
		}
	}
}
