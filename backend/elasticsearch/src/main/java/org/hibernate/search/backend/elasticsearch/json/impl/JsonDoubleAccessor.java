package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;


public class JsonDoubleAccessor extends AbstractTypingJsonAccessor<Double> {

	public JsonDoubleAccessor(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<Double> getExpectedElementType() {
		return JsonElementTypes.DOUBLE;
	}

}
