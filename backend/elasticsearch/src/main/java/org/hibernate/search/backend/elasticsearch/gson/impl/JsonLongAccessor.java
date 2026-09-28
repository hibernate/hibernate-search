package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonElement;

public class JsonLongAccessor extends AbstractTypingJsonAccessor<Long> {

	public JsonLongAccessor(JsonAccessor<JsonElement> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<Long> getExpectedElementType() {
		return JsonElementTypes.LONG;
	}

}
