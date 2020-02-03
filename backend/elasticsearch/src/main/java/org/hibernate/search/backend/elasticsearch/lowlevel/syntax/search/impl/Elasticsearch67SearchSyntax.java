/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Elasticsearch67SearchSyntax extends Elasticsearch7SearchSyntax {

	private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "docvalue_fields" ).asArray();

	private static final JsonAccessor<JsonElement> FIELD_ACCESSOR =
			JsonAccessor.root().property( "field" );

	private static final JsonAccessor<JsonElement> FORMAT_ACCESSOR =
			JsonAccessor.root().property( "format" );

	private static final JsonPrimitive USE_FIELD_MAPPING_FORMAT_JSON =
			new JsonPrimitive( "use_field_mapping" );

	@Override
	public void requestDocValues(JsonObject requestBody, JsonPrimitive fieldName) {
		// Elasticsearch 6.7/6.8 will issue a warning if we request doc values without specifying a format,
		// even for string types where the format does not make sense.
		// So we specify a format just to avoid that warning...
		JsonObject docValuesRequest = new JsonObject();
		FIELD_ACCESSOR.set( docValuesRequest, fieldName );
		FORMAT_ACCESSOR.set( docValuesRequest, USE_FIELD_MAPPING_FORMAT_JSON );

		DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, docValuesRequest );
	}
}
