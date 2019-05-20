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

		if ( new BigDecimal( value ).multiply( scalingFactor ).compareTo( BigDecimal.valueOf( Long.MAX_VALUE ) ) > 0 ) {
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
}
