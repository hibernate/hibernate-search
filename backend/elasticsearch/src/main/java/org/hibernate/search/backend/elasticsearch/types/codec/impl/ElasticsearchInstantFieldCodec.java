/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.time.Instant;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class ElasticsearchInstantFieldCodec implements ElasticsearchFieldCodec<Instant> {

	public static final ElasticsearchInstantFieldCodec INSTANCE = new ElasticsearchInstantFieldCodec();

	private ElasticsearchInstantFieldCodec() {
	}

	@Override
	public JsonElement encode(Instant value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive( value.toEpochMilli() );
	}

	@Override
	public Instant decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		Long time = JsonElementTypes.LONG.fromElement( element );
		return Instant.ofEpochMilli( time );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
