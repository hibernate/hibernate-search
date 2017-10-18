/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldFormatter;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class SourceHitExtractor implements HitExtractor<Object> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asObject();

	private final String absoluteFieldPath;
	private final UnknownTypeJsonAccessor hitFieldValueAccessor;
	private final ElasticsearchFieldFormatter formatter;

	public SourceHitExtractor(String absoluteFieldPath, ElasticsearchFieldFormatter formatter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.hitFieldValueAccessor = HIT_SOURCE_ACCESSOR.property( absoluteFieldPath );
		this.formatter = formatter;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		JsonArray source = REQUEST_SOURCE_ACCESSOR.get( requestBody )
				.orElseGet( () -> {
					JsonArray newSource = new JsonArray();
					REQUEST_SOURCE_ACCESSOR.set( requestBody, newSource );
					return newSource;
				} );
		JsonPrimitive fieldPathJson = new JsonPrimitive( absoluteFieldPath );
		if ( !source.contains( fieldPathJson ) ) {
			source.add( fieldPathJson );
		}
	}

	@Override
	public Object extractHit(JsonObject responseBody, JsonObject hit) {
		JsonElement fieldValue = hitFieldValueAccessor.get( hit ).orElse( null );
		return formatter.parse( fieldValue );
	}

}
