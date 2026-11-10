package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;


public class JsonLongAccessor extends AbstractTypingJsonAccessor<Long> {

	public JsonLongAccessor(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<Long> getExpectedElementType() {
		return JsonElementTypes.LONG;
	}

}
