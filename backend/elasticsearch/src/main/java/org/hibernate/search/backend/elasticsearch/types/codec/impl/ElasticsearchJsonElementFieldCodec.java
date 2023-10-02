/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class ElasticsearchJsonElementFieldCodec implements ElasticsearchFieldCodec<JsonElement> {

	private final Gson gson;

	public ElasticsearchJsonElementFieldCodec(Gson gson) {
		this.gson = gson;
	}

	@Override
	public JsonElement encode(JsonElement value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return value;
	}

	@Override
	public JsonElement decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return element;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		if ( other == this ) {
			return true;
		}
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}

		ElasticsearchJsonElementFieldCodec castedOther = (ElasticsearchJsonElementFieldCodec) other;
		return gson.equals( castedOther.gson );
	}
}
