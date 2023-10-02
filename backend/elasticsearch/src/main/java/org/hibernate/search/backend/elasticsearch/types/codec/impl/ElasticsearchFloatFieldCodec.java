/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchFloatFieldCodec implements ElasticsearchFieldCodec<Float> {
	public static final ElasticsearchFloatFieldCodec INSTANCE = new ElasticsearchFloatFieldCodec();

	private ElasticsearchFloatFieldCodec() {
	}

	@Override
	public JsonElement encode(Float value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( value );
	}

	@Override
	public Float decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return JsonElementTypes.FLOAT.fromElement( element );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
