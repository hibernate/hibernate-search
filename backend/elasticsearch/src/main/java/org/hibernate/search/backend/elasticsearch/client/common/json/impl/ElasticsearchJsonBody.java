package org.hibernate.search.backend.elasticsearch.client.common.json.impl;

import java.util.List;

import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonValidation;
import org.hibernate.search.util.json.spi.JsonBody;

import tools.jackson.databind.JsonNode;

/** Shared bounded framing: each part has one final newline; an empty list emits no bytes. */
public final class ElasticsearchJsonBody {
	private ElasticsearchJsonBody() {
	}

	/** Internal caller contract: parts are already parsed/built/validated, ordinary and acyclic. Retain stable submitted trees; all parts share one workspace and one whole-body length probe. */
	public static JsonBody ndjson(List<? extends JsonNode> parts) {
		return ndjson( parts, JsonValidation.systemDefault() );
	}

	public static JsonBody ndjson(List<? extends JsonNode> parts, JsonValidation validation) {
		return JsonBodies.ndjsonInternal( parts, validation );
	}
}
