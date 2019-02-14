/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchYearFieldCodec implements ElasticsearchFieldCodec<Year> {

	private final DateTimeFormatter formatter;

	public ElasticsearchYearFieldCodec(DateTimeFormatter delegate) {
		this.formatter = delegate;
	}

	@Override
	public JsonElement encode(Year value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( formatter.format( value ) );
	}

	@Override
	public Year decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		String stringValue = JsonElementTypes.STRING.fromElement( element );
		return Year.parse( stringValue, formatter );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj.getClass() != getClass() ) {
			return false;
		}

		ElasticsearchYearFieldCodec other = (ElasticsearchYearFieldCodec) obj;

		return formatter.equals( other.formatter );
	}
}
