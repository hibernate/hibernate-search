/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Helpers allowing to log JSON.
 *
 * @author Yoann Rodiere
 */
public class JsonLogHelper {

	private static final Gson GSON_PRETTY_PRINT = new GsonBuilder().setPrettyPrinting().create();

	private JsonLogHelper() {
		// Private constructor
	}

	public static String prettyPrint(JsonObject object) {
		StringBuilder stringWriter = new StringBuilder();
		prettyPrint( stringWriter, object );
		return stringWriter.toString();
	}

	public static void prettyPrint(StringBuilder sb, JsonObject object) {
		try {
			GSON_PRETTY_PRINT.toJson( object, sb );
		}
		catch (RuntimeException e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace( new PrintWriter( writer ) );
			sb.append( writer.toString() );
		}
	}

	public static String prettyPrint(Iterable<JsonObject> objects) {
		StringBuilder sb = new StringBuilder( 180 );
		prettyPrint( sb, objects );
		return sb.toString();
	}

	public static void prettyPrint(StringBuilder sb, Iterable<JsonObject> objects) {
		for ( JsonObject object : objects ) {
			prettyPrint( sb, object );
			sb.append("\n");
		}
	}

	public static JsonElement property(JsonObject parent, String name) {
		if ( parent == null ) {
			return null;
		}
		return parent.get( name );
	}

	public static String propertyAsString(JsonElement parent, String name) {
		if ( parent == null || !parent.isJsonObject() ) {
			return null;
		}
		JsonElement propretyValue = property( parent.getAsJsonObject(), name );
		if ( propretyValue == null ) {
			return null;
		}
		return propretyValue.toString(); // Also support non-string properties
	}
}
