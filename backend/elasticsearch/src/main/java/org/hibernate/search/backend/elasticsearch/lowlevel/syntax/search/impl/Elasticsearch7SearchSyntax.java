/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The search syntax for ES7.0 and later.
 */
public class Elasticsearch7SearchSyntax implements ElasticsearchSearchSyntax {

	private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "docvalue_fields" ).asArray();

	private static final JsonAccessor<JsonElement> NESTED_ACCESSOR = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH_ACCESSOR = JsonAccessor.root().property( "path" );
	private static final JsonAccessor<JsonElement> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" );

	@Override
	public String getTermAggregationOrderByTermToken() {
		return "_key";
	}

	@Override
	public void requestDocValues(JsonObject requestBody, JsonPrimitive fieldName) {
		// The default format is the format defined in the mapping, which is what we want
		DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, fieldName );
	}

	@Override
	public void requestNestedSort(List<String> nestedPathHierarchy, JsonObject innerObject, JsonObject filterOrNull) {
		JsonObject nextNestedObjectTarget = innerObject;
		for ( int i = 0; i < nestedPathHierarchy.size(); i++ ) {
			String nestedPath = nestedPathHierarchy.get( i );

			JsonObject nestedObject = new JsonObject();
			PATH_ACCESSOR.set( nestedObject, new JsonPrimitive( nestedPath ) );
			NESTED_ACCESSOR.set( nextNestedObjectTarget, nestedObject );
			if ( i == (nestedPathHierarchy.size() - 1) && filterOrNull != null ) {
				FILTER_ACCESSOR.set( nestedObject, filterOrNull );
			}

			// the new api requires a recursion on the path hierarchy
			nextNestedObjectTarget = nestedObject;
		}
	}
}
