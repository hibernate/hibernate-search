package org.hibernate.search.backend.elasticsearch.client.common.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponseBody;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.node.ObjectNode;

/** Backend-owned parsed result. Native backend processing and public handler access share one tree. */
public final class ElasticsearchJsonResponseBody implements ElasticsearchResponseBody {
	private final ObjectNode body;
	private final JsonValidation validation;

	public ElasticsearchJsonResponseBody(ObjectNode body, JsonValidation validation) {
		this.body = body;
		this.validation = Objects.requireNonNull( validation );
		if ( body != null ) {
			validation.validateInternal( body );
		}
	}

	public ObjectNode bodyJson() {
		return body;
	}

	@Override
	public <T> T body(JsonHandler<T> handler) {
		return JsonHandlerBridge.exportObject( handler, body, "response body export", validation );
	}
}
