/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchLongFieldCodec implements ElasticsearchFieldCodec<Long> {
	public static final ElasticsearchLongFieldCodec INSTANCE = new ElasticsearchLongFieldCodec();

	private ElasticsearchLongFieldCodec() {
	}

	@Override
	public JsonElement encode(Long value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( value );
	}

	@Override
	public JsonElement encodeForAggregation(ElasticsearchSearchSyntax searchSyntax, Long value) {
		return searchSyntax.encodeLongForAggregation( value );
	}

	@Override
	public Long decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return JsonElementTypes.LONG.fromElement( element );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
