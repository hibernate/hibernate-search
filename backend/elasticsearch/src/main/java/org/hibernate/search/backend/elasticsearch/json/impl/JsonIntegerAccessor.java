package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;


public class JsonIntegerAccessor extends AbstractTypingJsonAccessor<Integer> {

	public JsonIntegerAccessor(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<Integer> getExpectedElementType() {
		return JsonElementTypes.INTEGER;
	}

}
