package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.logging.impl.IndexingLog;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.engine.cfg.spi.NumberUtils;
import org.hibernate.search.util.json.internal.spi.JsonNumbers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public class ElasticsearchBigIntegerFieldCodec extends AbstractElasticsearchFieldCodec<BigInteger> {

	private final int decimalScale;
	private final BigDecimal scalingFactor;
	private final BigDecimal minScaledValue;
	private final BigDecimal maxScaledValue;

	public ElasticsearchBigIntegerFieldCodec(JsonCodecService json, int decimalScale) {
		super( json );
		this.decimalScale = decimalScale;
		scalingFactor = BigDecimal.TEN.pow( decimalScale, new MathContext( 10, RoundingMode.HALF_UP ) );
		minScaledValue = new BigDecimal( NumberScaleConstants.MIN_LONG_AS_BIGINTEGER, decimalScale );
		maxScaledValue = new BigDecimal( NumberScaleConstants.MAX_LONG_AS_BIGINTEGER, decimalScale );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "decimalScale=" + decimalScale
				+ "]";
	}

	@Override
	public JsonNode encode(BigInteger value) {
		if ( value == null ) {
			return NullNode.instance;
		}

		BigDecimal decimal = new BigDecimal( value );
		if ( decimal.compareTo( minScaledValue ) < 0 || decimal.compareTo( maxScaledValue ) > 0 ) {
			throw IndexingLog.INSTANCE.scaledNumberTooLarge( value, minScaledValue, maxScaledValue );
		}

		return JsonScalarConversions.toNode( value );
	}

	@Override
	public BigInteger decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}

		return JsonElementTypes.BIG_INTEGER.fromElement( element );
	}

	@Override
	public BigInteger decode(Double value) {
		return NumberUtils.toBigInteger( value );
	}

	@Override
	public BigInteger decodeAggregationKey(JsonNode key, JsonNode keyAsString) {
		if ( key == null || key.isNull() ) {
			return null;
		}

		// scaled_float aggregations format keys in double format, e.g. 1.514843E8
		return JsonNumbers
				.truncateToBigInteger( JsonElementTypes.BIG_DECIMAL.fromElement( key ) );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}

		ElasticsearchBigIntegerFieldCodec other = (ElasticsearchBigIntegerFieldCodec) obj;
		return decimalScale == other.decimalScale;
	}

	public BigDecimal scalingFactor() {
		return scalingFactor;
	}

}
