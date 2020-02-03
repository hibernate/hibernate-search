/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The search syntax for ES7.0 and later.
 */
public class Elasticsearch7SearchSyntax implements ElasticsearchSearchSyntax {

	private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "docvalue_fields" ).asArray();

	@Override
	public String getTermAggregationOrderByTermToken() {
		return "_key";
	}

	@Override
	public boolean useOldSortNestedApi() {
		return false;
	}

	@Override
	public void requestDocValues(JsonObject requestBody, JsonPrimitive fieldName) {
		// The default format is the format defined in the mapping, which is what we want
		DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, fieldName );
	}
}
