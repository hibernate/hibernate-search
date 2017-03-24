/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchClientUtils {

	private static final ContentType JSON_CONTENT_TYPE = ContentType.APPLICATION_JSON.withCharset("utf-8");

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) {
		List<JsonObject> bodyParts = request.getBodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, builder );
			builder.append("\n");
		}
		return new StringEntity( builder.toString(), JSON_CONTENT_TYPE );
	}

	public static JsonObject parseJsonResponse(GsonProvider gsonProvider, Response response) throws IOException {
		HttpEntity entity = response.getEntity();
		if ( entity == null ) {
			return null;
		}

		Gson gson = gsonProvider.getGson();
		ContentType contentType = ContentType.get( entity );
		try ( InputStream inputStream = entity.getContent();
				Reader reader = new InputStreamReader( inputStream, contentType.getCharset() ) ) {
			return gson.fromJson( reader, JsonObject.class );
		}
	}

	public static String formatRequest(GsonProvider gsonProvider, ElasticsearchRequest request) {
		StringBuilder sb = new StringBuilder();

		sb.append( "Method: " ).append( request.getMethod() ).append( "\n" );
		sb.append( "Path: " ).append( request.getPath() ).append( "\n" );

		sb.append( "Data:\n" );
		sb.append( formatRequestData( gsonProvider, request ) );
		sb.append( "\n" );
		return sb.toString();
	}

	public static String formatRequestData(GsonProvider gsonProvider, ElasticsearchRequest request) {
		List<JsonObject> bodyParts = request.getBodyParts();
		Gson gson = gsonProvider.getGsonPrettyPrinting();
		StringBuilder builder = new StringBuilder();
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, builder );
			builder.append("\n");
		}
		return builder.toString();
	}

	public static String formatRequestData(GsonProvider gsonProvider, JsonObject body) {
		Gson gson = gsonProvider.getGsonPrettyPrinting();
		return gson.toJson( body );
	}

	public static String formatResponse(GsonProvider gsonProvider, Response response, JsonObject parsedResponse) {
		StringBuilder sb = new StringBuilder();
		StatusLine statusLine = response.getStatusLine();
		sb.append( "Status: " ).append( statusLine.getStatusCode() ).append( " " ).append( statusLine.getReasonPhrase() ).append( "\n" );
		sb.append( "Error message: " ).append( propertyAsString( parsedResponse, "error" ) ).append( "\n" );
		sb.append( "Cluster name: " ).append( propertyAsString( parsedResponse, "cluster_name" ) ).append( "\n" );
		sb.append( "Cluster status: " ).append( propertyAsString( parsedResponse, "status" ) ).append( "\n" );
		sb.append( "\n" );

		JsonElement items = property( parsedResponse, "items" );
		if ( items != null && items.isJsonArray() ) {
			for ( JsonElement item : items.getAsJsonArray() ) {
				for ( Map.Entry<String, JsonElement> entry : item.getAsJsonObject().entrySet() ) {
					sb.append( "Operation: " ).append( entry.getKey() ).append( "\n" );
					JsonElement value = entry.getValue();
					sb.append( "  Index: " ).append( propertyAsString( value, "_index" ) ).append( "\n" );
					sb.append( "  Type: " ).append( propertyAsString( value, "_type" ) ).append( "\n" );
					sb.append( "  Id: " ).append( propertyAsString( value, "_id" ) ).append( "\n" );
					sb.append( "  Status: " ).append( propertyAsString( value, "status" ) ).append( "\n" );
					sb.append( "  Error: " ).append( propertyAsString( value, "error" ) ).append( "\n" );
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
		JsonElement propretyValue = property( parent.getAsJsonObject(), name );
		if ( propretyValue == null ) {
			return null;
		}
		return propretyValue.toString(); // Also support non-string properties
	}
}
