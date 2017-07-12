/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchClientUtils {

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) {
		final List<JsonObject> bodyParts = request.getBodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		return new GsonHttpEntity( gson, bodyParts );
	}

	public static String formatRequest(GsonProvider gsonProvider, ElasticsearchRequest request) {
		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		StringBuilder sb = new StringBuilder( 180 );

		sb.append( "Method: " ).append( request.getMethod() );
		sb.append( "\nPath: " ).append( request.getPath() );
		sb.append( "\nData:\n" );
		appendRequestData( sb, gsonProvider, request );
		sb.append( "\n" );
		return sb.toString();
	}

	private static void appendRequestData(StringBuilder sb, GsonProvider gsonProvider, ElasticsearchRequest request) {
		List<JsonObject> bodyParts = request.getBodyParts();
		Gson gson = gsonProvider.getGsonPrettyPrinting();
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, sb );
			sb.append("\n");
		}
	}

	public static String formatRequestData(GsonProvider gsonProvider, ElasticsearchRequest request) {
		StringBuilder sb = new StringBuilder( 180 );
		appendRequestData( sb, gsonProvider, request );
		return sb.toString();
	}

	public static String formatResponse(GsonProvider gsonProvider, ElasticsearchResponse response) {
		if ( response == null ) {
			return null;
		}
		JsonObject body = response.getBody();
		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		//Also useful to hint the builder to use larger increment steps.
		StringBuilder sb = new StringBuilder( 180 );
		sb.append( "Status: " ).append( response.getStatusCode() ).append( " " ).append( response.getStatusMessage() );
		sb.append( "\nError message: " ).append( propertyAsString( body, "error" ) );
		sb.append( "\nCluster name: " ).append( propertyAsString( body, "cluster_name" ) );
		sb.append( "\nCluster status: " ).append( propertyAsString( body, "status" ) );
		sb.append( "\n\n" );

		JsonElement items = property( body, "items" );
		if ( items != null && items.isJsonArray() ) {
			for ( JsonElement item : items.getAsJsonArray() ) {
				for ( Map.Entry<String, JsonElement> entry : item.getAsJsonObject().entrySet() ) {
					sb.append( "Operation: " ).append( entry.getKey() );
					JsonElement value = entry.getValue();
					sb.append( "\n  Index: " ).append( propertyAsString( value, "_index" ) );
					sb.append( "\n  Type: " ).append( propertyAsString( value, "_type" ) );
					sb.append( "\n  Id: " ).append( propertyAsString( value, "_id" ) );
					sb.append( "\n  Status: " ).append( propertyAsString( value, "status" ) );
					sb.append( "\n  Error: " ).append( propertyAsString( value, "error" ) );
					sb.append( "\n" );
				}
			}
		}

		return sb.toString();
	}

	private static JsonElement property(JsonObject parent, String name) {
		if ( parent == null ) {
			return null;
		}
		return parent.get( name );
	}

	private static String propertyAsString(JsonElement parent, String name) {
		if ( parent == null || !parent.isJsonObject() ) {
			return null;
		}
		JsonElement propertyValue = property( parent.getAsJsonObject(), name );
		if ( propertyValue == null ) {
			return null;
		}
		return propertyValue.toString(); // Also support non-string properties
	}
}
