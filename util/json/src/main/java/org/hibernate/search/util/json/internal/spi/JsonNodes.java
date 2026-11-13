package org.hibernate.search.util.json.internal.spi;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** Shared node factories, also usable as noncapturing method references. */
public final class JsonNodes {

	private JsonNodes() {
	}

	public static ObjectNode objectNode() {
		return JsonNodeFactory.instance.objectNode();
	}

	public static ArrayNode arrayNode() {
		return JsonNodeFactory.instance.arrayNode();
	}
}
