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
 * The search syntax for ES6.0 to 6.3.
 */
public class Elasticsearch60SearchSyntax extends Elasticsearch64SearchSyntax {

	private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "docvalue_fields" ).asArray();

	@Override
	public void requestDocValues(JsonObject requestBody, JsonPrimitive fieldName) {
		// Elasticsearch 5 to 6.3 doesn't allow to specify a format,
		// but we only request doc values for String field which do not require a format,
		// and unlike 6.7/6.8, there is no warning when we do not specify a format.
		// So we just don't specify a format.
		DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, fieldName );
	}
}
