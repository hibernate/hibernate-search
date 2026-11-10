package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public abstract class AbstractElasticsearchJavaTimeFieldCodec<T extends TemporalAccessor>
		extends AbstractElasticsearchFieldCodec<T> {

	protected final DateTimeFormatter formatter;

	public AbstractElasticsearchJavaTimeFieldCodec(JsonCodecService json, DateTimeFormatter delegate) {
		super( json );
		this.formatter = delegate;
	}

	@Override
	public JsonNode encode(T value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		return JsonScalarConversions.toNode( nullUnsafeFormat( value ) );
	}

	/**
	 * A different encoding is required for provided missing Java time values. See HSEARCH-3255.
	 *
	 * @param value to encode
	 * @return a {@link JsonNode} containing the long scalar value, if {@code value} is not null.
	 */
	@Override
	public JsonNode encodeForMissing(T value) {
		if ( value == null ) {
			return null;
		}

		return JsonScalarConversions.toNode( nullUnsafeScalar( value ) );
	}

	@Override
	public T decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}
		return nullUnsafeParse( JsonElementTypes.STRING.fromElement( element ) );
	}

	@Override
	public T decodeAggregationKey(JsonNode key, JsonNode keyAsString) {
		return decode( keyAsString );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj.getClass() != getClass() ) {
			return false;
		}

		AbstractElasticsearchJavaTimeFieldCodec<?> other = (AbstractElasticsearchJavaTimeFieldCodec<?>) obj;
		return formatter.equals( other.formatter );
	}

	protected String nullUnsafeFormat(T value) {
		return formatter.format( value );
	}

	protected abstract T nullUnsafeParse(String stringValue);

	protected abstract Long nullUnsafeScalar(T value);
}
