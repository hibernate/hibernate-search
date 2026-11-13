package org.hibernate.search.util.json.internal.spi;

import org.hibernate.search.util.common.annotation.Incubating;

import tools.jackson.databind.JsonNode;

/** Explicit tree codec. Implementations construct known types without runtime field inspection. */
@Incubating
public interface JsonCodec<T> {
	T decode(JsonNode value);

	JsonNode encode(T value, boolean serializeNulls);
}
