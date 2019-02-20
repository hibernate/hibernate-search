/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class ElasticsearchCharacterFieldCodec implements ElasticsearchFieldCodec<Character> {
	// Must be a singleton so that equals() works as required by the interface
	public static final ElasticsearchCharacterFieldCodec INSTANCE = new ElasticsearchCharacterFieldCodec();

	private ElasticsearchCharacterFieldCodec() {
	}

	@Override
	public JsonElement encode(Character value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
		}
		return JsonElementTypes.CHARACTER.toElement( value );
	}

	@Override
	public Character decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return JsonElementTypes.CHARACTER.fromElement( element );
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {
		return INSTANCE == other;
	}
}
