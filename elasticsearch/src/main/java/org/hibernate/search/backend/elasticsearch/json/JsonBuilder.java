/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.json;

import java.util.Date;

import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Builder used to make the Gson API chainable.
 *
 * @author Guillaume Smet
 */
public class JsonBuilder {

	private JsonBuilder() {
	}

	public static JsonBuilder.Array array() {
		return new JsonBuilder.Array();
	}

	public static JsonBuilder.Array array(JsonArray jsonArray) {
		return new JsonBuilder.Array(jsonArray);
	}

	public static JsonBuilder.Object object() {
		return new JsonBuilder.Object();
	}

	public static JsonBuilder.Object object(JsonObject jsonObject) {
		return new JsonBuilder.Object( jsonObject );
	}

	public static class Array {
		private JsonArray jsonArray = new JsonArray();

		private Array() {
		}

		private Array(JsonArray jsonArray) {
			this.jsonArray = jsonArray;
		}

		public Array add(JsonElement element) {
			jsonArray.add( element );
			return this;
		}

		public Array add(JsonBuilder.Array element) {
			jsonArray.add( element.build() );
			return this;
		}

		public Array add(JsonBuilder.Object element) {
			jsonArray.add( element.build() );
			return this;
		}

		public JsonArray build() {
			return jsonArray;
		}

		@Override
		public String toString() {
			return jsonArray.toString();
		}

		public int size() {
			return jsonArray.size();
		}
	}

	public static class Object {
		private JsonObject jsonObject = new JsonObject();

		private Object() {
		}

		private Object(JsonObject jsonObject) {
			this.jsonObject = jsonObject;
		}

		public JsonBuilder.Object add( String property, JsonElement element) {
			jsonObject.add( property, element );
			return this;
		}

		public JsonBuilder.Object add( String property, JsonBuilder.Array element) {
			jsonObject.add( property, element.build() );
			return this;
		}

		public JsonBuilder.Object add( String property, JsonBuilder.Object element) {
			jsonObject.add( property, element.build() );
			return this;
		}

		public JsonBuilder.Object addProperty(String property, java.lang.Object value) {
			if ( value instanceof String || value == null ) {
				jsonObject.addProperty( property, (String) value );
			}
			else if ( value instanceof Number ) {
				jsonObject.addProperty( property, (Number) value );
			}
			else if ( value instanceof Boolean ) {
				jsonObject.addProperty( property, (Boolean) value );
			}
			else if ( value instanceof Character ) {
				jsonObject.addProperty( property, (Character) value );
			}
			else if (value instanceof Date) {
				// we use this Gson formatter to be sure the date format is in sync with the JestClient configuration
				jsonObject.addProperty( property, JestClient.GSON.toJson( value ).replace( "\"", "" ) );
			}
			// TODO GSM: see if we have to support Calendar here as Gson serializes Calendar in a weird fashion
			else {
				jsonObject.addProperty( property, value.toString() );
			}
			return this;
		}

		public JsonBuilder.Object addProperty(String property, Boolean value) {
			jsonObject.addProperty( property, value );
			return this;
		}

		public JsonBuilder.Object addProperty(String property, Number value) {
			jsonObject.addProperty( property, value );
			return this;
		}

		public JsonBuilder.Object addProperty(String property, Character value) {
			jsonObject.addProperty( property, value );
			return this;
		}

		public JsonBuilder.Object addProperty(String property, String value) {
			jsonObject.addProperty( property, value );
			return this;
		}

		public JsonObject build() {
			return jsonObject;
		}

		@Override
		public String toString() {
			return jsonObject.toString();
		}

	}

}
