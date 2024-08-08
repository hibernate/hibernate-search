/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public final class ElasticsearchBooleanFieldCodec extends AbstractElasticsearchFieldCodec<Boolean> {

	public ElasticsearchBooleanFieldCodec(Gson gson) {
		super( gson );
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
	public Boolean decode(Double value) {
		return value != 0;
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
		return other instanceof ElasticsearchBooleanFieldCodec;
	}

	@Override
	public String fromJsonElementToString(JsonElement value) {
		if ( value == null || value.isJsonNull() ) {
			return null;
		}
		// Sometimes the backend may return boolean as `true` and sometimes as `"true"`,
		//  hence we just get the boolean value and convert it to String as:
		return Boolean.toString( value.getAsBoolean() );
	}
}
