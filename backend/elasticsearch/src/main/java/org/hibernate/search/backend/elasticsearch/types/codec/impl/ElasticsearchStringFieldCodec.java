/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchStringFieldCodec implements ElasticsearchFieldCodec<String> {
	// Must be a singleton so that equals() works as required by the interface
	public static final ElasticsearchStringFieldCodec INSTANCE = new ElasticsearchStringFieldCodec();

	private ElasticsearchStringFieldCodec() {
	}

	@Override
	public JsonElement encode(String value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( value );
	}

	@Override
	public String decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return JsonElementTypes.STRING.fromElement( element );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
