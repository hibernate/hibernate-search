package org.hibernate.search.backend.elasticsearch.client.common.impl;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.node.ObjectNode;

/** Backend-native view of the public transport response; metadata and parsed result are retained directly. */
public final class ElasticsearchJsonResponse extends ElasticsearchResponse<ElasticsearchJsonResponseBody> {
	public ElasticsearchJsonResponse(ElasticsearchResponse<ElasticsearchJsonResponseBody> response, JsonValidation validation) {
		super( response.hostAndPort(), response.statusCode(), response.statusMessage(),
				response.result() == null ? new ElasticsearchJsonResponseBody( null, validation ) : response.result() );
	}

	private ElasticsearchJsonResponse(String host, int status, String message, ObjectNode body, JsonValidation validation) {
		super( host, status, message, new ElasticsearchJsonResponseBody( body, validation ) );
	}

	public static ElasticsearchJsonResponse withJsonBody(String host, int status, String message, ObjectNode body) {
		if ( body != null ) {
			JsonTreeValidation.validateExternal( body );
		}
		return withInternalJsonBody( host, status, message, body );
	}

	public static ElasticsearchJsonResponse withInternalJsonBody(String host, int status, String message, ObjectNode body) {
		return withInternalJsonBody( host, status, message, body, JsonValidation.systemDefault() );
	}

	public static ElasticsearchJsonResponse withInternalJsonBody(String host, int status, String message, ObjectNode body,
			JsonValidation validation) {
		return new ElasticsearchJsonResponse( host, status, message, body, validation );
	}

	public ObjectNode bodyJson() {
		return result().bodyJson();
	}

	public <T> T body(JsonHandler<T> handler) {
		return result().body( handler );
	}
}
