/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.query.impl;

import java.util.Collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class Queries {

	private Queries() {
	}

	public static JsonObject matchAll() {
		JsonObject matchAll = new JsonObject();
		matchAll.add( "match_all", new JsonObject() );
		return matchAll;
	}

	public static JsonObject term(String absoluteFieldPath, String value) {
		JsonObject predicate = new JsonObject();

		JsonObject innerObject = new JsonObject();
		predicate.add( "term", innerObject );

		innerObject.addProperty( absoluteFieldPath, value );

		return predicate;
	}

	public static JsonObject anyTerm(String absoluteFieldPath, Collection<String> values) {
		JsonObject predicate = new JsonObject();

		JsonObject innerObject = new JsonObject();
		predicate.add( "terms", innerObject );

		JsonArray terms = new JsonArray();
		innerObject.add( absoluteFieldPath, terms );
		for ( String value : values ) {
			terms.add( value );
		}

		return predicate;
	}

	public static JsonObject boolFilter(JsonObject must, JsonArray filters) {
		if ( filters == null || filters.isEmpty() ) {
			return must;
		}

		JsonObject predicate = new JsonObject();
		JsonObject innerObject = new JsonObject();
		predicate.add( "bool", innerObject );

		if ( must != null ) {
			innerObject.add( "must", must );
		}
		innerObject.add( "filter", filters );

		return predicate;
	}

	public static JsonObject boolCombineMust(JsonObject must, JsonArray otherMustClauses) {
		if ( otherMustClauses == null || otherMustClauses.isEmpty() ) {
			return must;
		}
		if ( must == null && otherMustClauses.size() == 1 ) {
			return otherMustClauses.get( 0 ).getAsJsonObject();
		}

		JsonObject predicate = new JsonObject();
		JsonObject innerObject = new JsonObject();
		predicate.add( "bool", innerObject );
		JsonArray mustClauses = new JsonArray();
		if ( must != null ) {
			mustClauses.add( must );
		}
		mustClauses.addAll( otherMustClauses );
		innerObject.add( "must", mustClauses );

		return predicate;
	}
}
