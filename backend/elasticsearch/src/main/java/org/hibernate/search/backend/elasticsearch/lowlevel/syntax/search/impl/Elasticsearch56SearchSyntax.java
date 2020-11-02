/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The search syntax for ES5.6 to 6.2.
 */
public class Elasticsearch56SearchSyntax extends Elasticsearch60SearchSyntax {
	private static final JsonAccessor<JsonElement> NESTED_PATH_ACCESSOR = JsonAccessor.root().property( "nested_path" );
	private static final JsonAccessor<JsonElement> NESTED_FILTER_ACCESSOR = JsonAccessor.root().property( "nested_filter" );

	@Override
	public String getTermAggregationOrderByTermToken() {
		return "_term"; // _key in ES6.0+
	}

	@Override
	public void requestNestedSort(List<String> nestedPathHierarchy, JsonObject innerObject, JsonObject filterOrNull) {
		// the old api requires only the last path (the deepest one)
		String lastNestedPath = nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

		NESTED_PATH_ACCESSOR.set( innerObject, new JsonPrimitive( lastNestedPath ) );
		if ( filterOrNull != null ) {
			NESTED_FILTER_ACCESSOR.set( innerObject, filterOrNull );
		}
	}
}
