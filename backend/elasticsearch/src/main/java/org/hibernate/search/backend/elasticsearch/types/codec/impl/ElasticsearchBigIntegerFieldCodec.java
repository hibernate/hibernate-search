/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchBigIntegerFieldCodec implements ElasticsearchFieldCodec<BigInteger> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BigDecimal scalingFactor;

	public ElasticsearchBigIntegerFieldCodec(BigDecimal scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	@Override
	public JsonElement encode(BigInteger value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}

		if ( isTooLarge( value ) ) {
			throw log.scaledNumberTooLarge( value );
		}

		return new JsonPrimitive( value );
	}

	@Override
	public BigInteger decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}

		return JsonElementTypes.BIG_INTEGER.fromElement( element );
	}

	@Override
	public BigInteger decodeAggregationKey(JsonElement key, JsonElement keyAsString) {
		if ( key == null || key.isJsonNull() ) {
			return null;
		}

		// scaled_float aggregations format keys in double format, e.g. 1.514843E8
		return JsonElementTypes.BIG_DECIMAL.fromElement( key ).toBigInteger();
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
		// comparing only their numeric values, they can have different scales
		return scalingFactor.compareTo( other.scalingFactor ) == 0;
	}

	public boolean isTooLarge(BigInteger value) {
		BigDecimal scaled = new BigDecimal( value ).multiply( scalingFactor );
		return (
			scaled.compareTo( NumberScaleConstants.MIN_LONG_AS_BIGDECIMAL ) < 0 ||
			scaled.compareTo( NumberScaleConstants.MAX_LONG_AS_BIGDECIMAL ) > 0
		);
	}
}
