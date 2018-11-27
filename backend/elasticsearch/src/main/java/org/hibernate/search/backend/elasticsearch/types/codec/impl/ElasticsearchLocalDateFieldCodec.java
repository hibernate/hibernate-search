/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchLocalDateFieldCodec implements ElasticsearchFieldCodec<LocalDate> {

	private final DateTimeFormatter formatter;

	public ElasticsearchLocalDateFieldCodec(DateTimeFormatter delegate) {
		this.formatter = delegate;
	}

	@Override
	public JsonElement encode(LocalDate value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( formatter.format( value ) );
	}

	@Override
	public LocalDate decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		String stringValue = JsonElementTypes.STRING.fromElement( element );
		return LocalDate.parse( stringValue, formatter );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj.getClass() != getClass() ) {
			return false;
		}

		ElasticsearchLocalDateFieldCodec other = (ElasticsearchLocalDateFieldCodec) obj;

		return formatter.equals( other.formatter );
	}
}
