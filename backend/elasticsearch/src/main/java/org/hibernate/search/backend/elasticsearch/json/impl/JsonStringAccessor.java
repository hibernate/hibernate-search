package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;


public class JsonStringAccessor extends AbstractTypingJsonAccessor<String> {

	public JsonStringAccessor(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<String> getExpectedElementType() {
		return JsonElementTypes.STRING;
	}

}
