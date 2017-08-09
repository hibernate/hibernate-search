/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class JsonDrivenProjection extends FieldProjection {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final String absoluteName;

	public JsonDrivenProjection(String absoluteName) {
		super();
		this.absoluteName = absoluteName;
	}

	@Override
	public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
		JsonElement value = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), absoluteName );
		if ( value == null || value.isJsonNull() ) {
			return null;
		}

		// TODO: HSEARCH-2255 should we do it?
		if ( !value.isJsonPrimitive() ) {
			throw LOG.unsupportedProjectionOfNonJsonPrimitiveFields( value );
		}

		JsonPrimitive primitive = value.getAsJsonPrimitive();

		if ( primitive.isBoolean() ) {
			return primitive.getAsBoolean();
		}
		else if ( primitive.isNumber() ) {
			// TODO HSEARCH-2255 this will expose a Gson-specific Number implementation; Can we somehow return an Integer,
			// Long... etc. instead?
			return primitive.getAsNumber();
		}
		else if ( primitive.isString() ) {
			return primitive.getAsString();
		}
		else {
			// TODO HSEARCH-2255 Better raise an exception?
			return primitive.toString();
		}
	}
}