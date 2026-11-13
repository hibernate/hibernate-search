package org.hibernate.search.backend.elasticsearch.testsupport;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody;
import org.hibernate.search.backend.elasticsearch.client.common.json.impl.JsonResponseBody;
import org.hibernate.search.backend.elasticsearch.client.common.json.impl.MappingJsonResponseConsumer;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;

/** Backend-owned fixtures: HTTP tests receive the same neutral bodies and consumers as external clients. */
public final class JsonClientTestSupport {
	private JsonClientTestSupport() {
	}

	public static <T> JsonBody ndjson(JsonHandler<T> handler, List<T> parts) {
		return ElasticsearchJsonBody.ndjson( parts.stream()
				.map( part -> JsonHandlerBridge.importObject( handler, part, "transport test fixture" ) ).toList(),
				JsonValidation.systemDefault() );
	}

	public static <T> JsonResponseConsumerFactory<T> responseFactory(JsonHandler<T> handler) {
		return (charset, limit) -> new MappingJsonResponseConsumer<>( new JsonResponseBody( charset, limit ),
				tree -> tree == null || tree.isNull() || tree.isMissingNode()
						? null
						: JsonHandlerBridge.exportValue( handler, tree, "transport test result",
								JsonValidation.systemDefault() ) );
	}
}
