/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.query;

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

	public static JsonObject boolFilter(JsonObject must, JsonObject filter) {
		if ( filter == null ) {
			return must;
		}

		JsonObject predicate = new JsonObject();
		JsonObject innerObject = new JsonObject();
		predicate.add( "bool", innerObject );

		if ( must != null ) {
			innerObject.add( "must", must );
		}
		innerObject.add( "filter", filter );

		return predicate;
	}
}
