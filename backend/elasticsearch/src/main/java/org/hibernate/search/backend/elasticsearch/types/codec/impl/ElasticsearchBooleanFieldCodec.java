/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchBooleanFieldCodec implements ElasticsearchFieldCodec<Boolean> {
	public static final ElasticsearchBooleanFieldCodec INSTANCE = new ElasticsearchBooleanFieldCodec();

	private ElasticsearchBooleanFieldCodec() {
	}

	@Override
	public JsonElement encode(Boolean value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( value );
	}

	@Override
	public Boolean decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return JsonElementTypes.BOOLEAN.fromElement( element );
	}

	@Override
	public Boolean decodeAggregationKey(JsonElement key, JsonElement keyAsString) {
		if ( key == null || key.isJsonNull() ) {
			return null;
		}
		int intValue = JsonElementTypes.INTEGER.fromElement( key );
		return intValue != 0;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
