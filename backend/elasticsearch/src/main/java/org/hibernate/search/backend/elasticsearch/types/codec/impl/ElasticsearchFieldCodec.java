/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;

import com.google.gson.JsonElement;

/**
 * Defines how a given value will be encoded as JSON and decoded from JSON.
 * <p>
 * Encodes values received from an {@link org.hibernate.search.engine.backend.document.IndexFieldReference} when indexing,
 * and returns decoded values to the {@link org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection}
 * when projecting in a search query.
 */
public interface ElasticsearchFieldCodec<F> {

	JsonElement encode(F value);

	default JsonElement encodeForMissing(F value) {
		return encode( value );
	}

	/**
	 * Encodes a value for inclusion in an aggregation request.
	 *
	 *
	 * @param searchSyntax The search syntax.
	 * @param value The value to encode.
	 * @return The encoded value.
	 */
	default JsonElement encodeForAggregation(ElasticsearchSearchSyntax searchSyntax, F value) {
		return encode( value );
	}

	F decode(JsonElement element);

	/**
	 * Decodes the key returned by a term aggregation.
	 * @param key The "key" property  returned by the aggregation.
	 * May be a number, a string, ... depending on the field type.
	 * @param keyAsString The "key_as_string" property returned by the term aggregation.
	 * Either null or a {@link com.google.gson.JsonPrimitive} containing a string.
	 * @return The decoded term.
	 */
	default F decodeAggregationKey(JsonElement key, JsonElement keyAsString) {
		return decode( key );
	}

	/**
	 * Determine whether another codec is compatible with this one, i.e. whether it will encode/decode the information
	 * to/from the document in a compatible way.
	 *
	 * @param other Another {@link ElasticsearchFieldCodec}, never {@code null}.
	 * @return {@code true} if the given codec is compatible. {@code false} otherwise, or when
	 * in doubt.
	 */
	boolean isCompatibleWith(ElasticsearchFieldCodec<?> other);

	/**
	 * Whether this codec can extract data from JsonArrays when decoding.
	 */
	default boolean canDecodeArrays() {
		return false;
	}
}
