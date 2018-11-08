/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.util.Date;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

/**
 * Encode and decode a {@link Date} to a json form.
 * Using a long number representing a milliseconds-since-the-epoch.
 */
public class UtilDateFieldCodec implements ElasticsearchFieldCodec<Date> {

	public static final UtilDateFieldCodec INSTANCE = new UtilDateFieldCodec();

	private UtilDateFieldCodec() {
	}

	@Override
	public JsonElement encode(Date value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( value.getTime() );
	}

	@Override
	public Date decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		Long time = JsonElementTypes.LONG.fromElement( element );
		return new Date( time );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
