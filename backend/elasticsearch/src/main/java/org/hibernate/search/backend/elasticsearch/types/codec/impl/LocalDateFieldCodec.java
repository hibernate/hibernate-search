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

public class LocalDateFieldCodec implements ElasticsearchFieldCodec {

	private final DateTimeFormatter delegate;

	public LocalDateFieldCodec(DateTimeFormatter delegate) {
		this.delegate = delegate;
	}

	@Override
	public JsonElement encode(Object object) {
		if ( object == null ) {
			return JsonNull.INSTANCE;
		}
		LocalDate value = (LocalDate) object;
		return new JsonPrimitive( delegate.format( value ) );
	}

	@Override
	public Object decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		String stringValue = JsonElementTypes.STRING.fromElement( element );
		return LocalDate.parse( stringValue, delegate );
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || obj.getClass() != getClass() ) {
			return false;
		}
		LocalDateFieldCodec other = (LocalDateFieldCodec) obj;
		return delegate.equals( other.delegate );
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

}
