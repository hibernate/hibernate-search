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
import com.google.gson.JsonPrimitive;

public class IntegerFieldCodec implements ElasticsearchFieldCodec {
	// Must be a singleton so that equals() works as required by the interface
	public static final IntegerFieldCodec INSTANCE = new IntegerFieldCodec();

	private IntegerFieldCodec() {
	}

	@Override
	public JsonElement encode(Object object) {
		if ( object == null ) {
			return JsonNull.INSTANCE;
		}
		Integer value = (Integer) object;
		return new JsonPrimitive( value );
	}

	@Override
	public Object decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return JsonElementTypes.INTEGER.fromElement( element );
	}
}
