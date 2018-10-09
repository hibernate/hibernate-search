/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class FieldSearchProjectionImpl<T> implements ElasticsearchSearchProjection<T> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asObject();

	private final String absoluteFieldPath;
	private final UnknownTypeJsonAccessor hitFieldValueAccessor;
	private final ElasticsearchFieldConverter converter;

	FieldSearchProjectionImpl(String absoluteFieldPath, ElasticsearchFieldConverter converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.hitFieldValueAccessor = HIT_SOURCE_ACCESSOR.property( absoluteFieldPath );
		this.converter = converter;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, ElasticsearchSearchQueryElementCollector elementCollector) {
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
	public void extract(ProjectionHitCollector collector, JsonObject responseBody, JsonObject hit) {
		JsonElement fieldValue = hitFieldValueAccessor.get( hit ).orElse( null );
		collector.collectProjection( converter.convertFromProjection( fieldValue ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( "]" );
		return sb.toString();
	}
}
