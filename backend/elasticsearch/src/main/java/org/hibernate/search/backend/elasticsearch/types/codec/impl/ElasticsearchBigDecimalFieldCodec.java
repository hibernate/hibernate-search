/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchBigDecimalFieldCodec implements ElasticsearchFieldCodec<BigDecimal> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int decimalScale;
	private final BigDecimal scalingFactor;
	private final BigDecimal minScaledValue;
	private final BigDecimal maxScaledValue;

	public ElasticsearchBigDecimalFieldCodec(int decimalScale) {
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
	public JsonElement encode(BigDecimal value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}

		if ( value.compareTo( minScaledValue ) < 0 || value.compareTo( maxScaledValue ) > 0 ) {
			throw log.scaledNumberTooLarge( value, minScaledValue, maxScaledValue );
		}

		return new JsonPrimitive( value );
	}

	@Override
	public BigDecimal decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}

		return JsonElementTypes.BIG_DECIMAL.fromElement( element );
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
