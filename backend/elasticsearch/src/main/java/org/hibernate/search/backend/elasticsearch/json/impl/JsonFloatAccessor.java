package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;


public class JsonFloatAccessor extends AbstractTypingJsonAccessor<Float> {

	public JsonFloatAccessor(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<Float> getExpectedElementType() {
		return JsonElementTypes.FLOAT;
	}

}
