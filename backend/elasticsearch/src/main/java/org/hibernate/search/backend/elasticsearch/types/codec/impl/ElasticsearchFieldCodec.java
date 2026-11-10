package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.JsonNode;

/**
 * Defines how a given value will be encoded as JSON and decoded from JSON.
 * <p>
 * Encodes values received from an {@link org.hibernate.search.engine.backend.document.IndexFieldReference} when indexing,
 * and returns decoded values to the {@link org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection}
 * when projecting in a search query.
 */
public interface ElasticsearchFieldCodec<F> {

	JsonNode encode(F value);

	default JsonNode encodeForMissing(F value) {
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
	default JsonNode encodeForAggregation(ElasticsearchSearchSyntax searchSyntax, F value) {
		return encode( value );
	}

	F decode(JsonNode element);

	default F decode(Double element) {
		throw new AssertionFailure( this + " codec is not expected to handle decoding metric aggregation values." );
	}

	/**
	 * Decodes the key returned by a term aggregation.
	 * @param key The "key" property  returned by the aggregation.
	 * May be a number, a string, ... depending on the field type.
	 * @param keyAsString The "key_as_string" property returned by the term aggregation.
	 * Either null or a {@link tools.jackson.databind.JsonNode} containing a string.
	 * @return The decoded term.
	 */
	default F decodeAggregationKey(JsonNode key, JsonNode keyAsString) {
		return decode( key );
	}

	default F decodeAggregationValue(Optional<Double> value, JsonNode valueAsString) {
		if ( valueAsString != null ) {
			return decode( valueAsString );
		}
		return value.map( this::decode ).orElse( null );
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
	 * Whether a JSON array can represent one field value, rather than a collection of field values.
	 * When indexing a multi-valued field with such a codec, an outer array separates its field values.
	 */
	default boolean canDecodeArrays() {
		return false;
	}

	JsonNode fromJsonStringToElement(String value);

	String fromJsonElementToString(JsonNode value);
}
