package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;


public interface UnknownTypeJsonAccessor extends JsonAccessor<JsonNode> {

	/**
	 * Repeatedly unwrap singleton arrays at this selected location for reads and traversal.
	 * Empty arrays are absent; arrays with multiple elements fail. Non-array values pass through.
	 * Traversal preserves wrappers. Setting this location replaces its raw value, including wrappers.
	 * Apply this operation to intermediate object paths as well as terminal scalar paths when needed.
	 * @return A new immutable adapted definition.
	 */
	UnknownTypeJsonAccessor unwrapSingletonArrays();

	JsonObjectAccessor asObject();

	JsonArrayAccessor asArray();

	JsonAccessor<String> asString();

	JsonAccessor<Boolean> asBoolean();

	JsonAccessor<Integer> asInteger();

	JsonAccessor<Long> asLong();

	JsonAccessor<Float> asFloat();

	JsonAccessor<Double> asDouble();

	UnknownTypeJsonAccessor element(int index);

	UnknownTypeJsonAccessor property(String propertyName);

}
