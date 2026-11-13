package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.logging.impl.IndexingLog;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.engine.cfg.spi.NumberUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public class ElasticsearchBigDecimalFieldCodec extends AbstractElasticsearchFieldCodec<BigDecimal> {

	private final int decimalScale;
	private final BigDecimal scalingFactor;
	private final BigDecimal minScaledValue;
	private final BigDecimal maxScaledValue;

	public ElasticsearchBigDecimalFieldCodec(JsonCodecService json, int decimalScale) {
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
	public JsonNode encode(BigDecimal value) {
		if ( value == null ) {
			return NullNode.instance;
		}

		if ( value.compareTo( minScaledValue ) < 0 || value.compareTo( maxScaledValue ) > 0 ) {
			throw IndexingLog.INSTANCE.scaledNumberTooLarge( value, minScaledValue, maxScaledValue );
		}

		return JsonScalarConversions.toNode( value );
	}

	@Override
	public BigDecimal decode(JsonNode element) {
		if ( element == null || element.isNull() ) {
			return null;
		}

		return JsonElementTypes.BIG_DECIMAL.fromElement( element );
	}

	@Override
	public BigDecimal decode(Double value) {
		return NumberUtils.toBigDecimal( value ).setScale( decimalScale, RoundingMode.FLOOR );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}

		ElasticsearchBigDecimalFieldCodec other = (ElasticsearchBigDecimalFieldCodec) obj;
		return decimalScale == other.decimalScale;
	}

	public BigDecimal scalingFactor() {
		return scalingFactor;
	}

}
