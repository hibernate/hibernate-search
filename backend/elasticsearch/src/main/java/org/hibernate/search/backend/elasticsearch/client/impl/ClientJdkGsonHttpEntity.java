/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

import org.hibernate.search.backend.elasticsearch.client.common.gson.entity.spi.GsonHttpEntityContentProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ClientJdkGsonHttpEntity extends GsonHttpEntityContentProvider implements HttpRequest.BodyPublisher {

	public static HttpRequest.BodyPublisher toEntity(Gson gson, ElasticsearchRequest request) throws IOException {
		final List<JsonObject> bodyParts = request.bodyParts();
		if ( bodyParts.isEmpty() ) {
			return HttpRequest.BodyPublishers.noBody();
		}
		return new ClientJdkGsonHttpEntity( gson, bodyParts );
	}

	private final HttpRequest.BodyPublisher delegate;

	public ClientJdkGsonHttpEntity(Gson gson, List<JsonObject> bodyParts) throws IOException {
		super( gson, bodyParts );
		delegate = HttpRequest.BodyPublishers.ofInputStream( this::getContent );
	}

	@Override
	public long contentLength() {
		return getContentLength();
	}

	@Override
	public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
		delegate.subscribe( subscriber );
	}

	static boolean isNoBodyPublisher(HttpRequest.BodyPublisher bodyPublisher) {
		return bodyPublisher.contentLength() == 0;
	}
}
