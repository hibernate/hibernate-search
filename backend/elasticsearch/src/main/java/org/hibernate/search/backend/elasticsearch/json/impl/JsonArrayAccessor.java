package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;


public interface JsonArrayAccessor extends JsonAccessor<ArrayNode> {
	UnknownTypeJsonAccessor element(int index);

	/**
	 * Add the given JsonNode to the array this accessor points to for the given {@code root},
	 * unless it is already present.
	 *
	 * @param root The root to be accessed.
	 * @param newValue The value to add.
	 * @throws UnexpectedJsonElementTypeException If an element in the path has unexpected type, preventing
	 * write access to the array this accessor points to.
	 */
	void addElementIfAbsent(ObjectNode root, JsonNode newValue);
}
