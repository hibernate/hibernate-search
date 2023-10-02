/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The search syntax for ES8.1 and later.
 */
public class Elasticsearch81SearchSyntax implements ElasticsearchSearchSyntax {

	private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "docvalue_fields" ).asArray();

	private static final JsonAccessor<JsonElement> NESTED_ACCESSOR = JsonAccessor.root().property( "nested" );
	private static final JsonAccessor<JsonElement> PATH_ACCESSOR = JsonAccessor.root().property( "path" );
	private static final JsonAccessor<JsonElement> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" );
	private static final JsonAccessor<Boolean> IGNORE_UNMAPPED_ACCESSOR =
			JsonAccessor.root().property( "ignore_unmapped" ).asBoolean();

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
			if ( i == ( nestedPathHierarchy.size() - 1 ) && filterOrNull != null ) {
				FILTER_ACCESSOR.set( nestedObject, filterOrNull );
			}

			// the new api requires a recursion on the path hierarchy
			nextNestedObjectTarget = nestedObject;
		}
	}

	@Override
	public void requestGeoDistanceSortIgnoreUnmapped(JsonObject innerObject) {
		IGNORE_UNMAPPED_ACCESSOR.set( innerObject, true );
	}

	@Override
	public JsonElement encodeLongForAggregation(Long value) {
		// https://github.com/elastic/elasticsearch/issues/81529 was solved in ES8.1
		return value == null ? JsonNull.INSTANCE : new JsonPrimitive( value );
	}
}
