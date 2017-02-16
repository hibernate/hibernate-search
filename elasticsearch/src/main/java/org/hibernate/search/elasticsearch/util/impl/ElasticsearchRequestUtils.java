/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import org.hibernate.search.elasticsearch.impl.GsonService;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import io.searchbox.action.Action;
import io.searchbox.action.DocumentTargetedAction;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchRequestUtils {

	private ElasticsearchRequestUtils() {
		// Private constructor
	}

	public static String formatRequest(GsonService gsonService, Action<?> action) {
		StringBuilder sb = new StringBuilder();

		sb.append( "Operation: " ).append( action.getClass().getSimpleName() ).append( "\n" );
		sb.append( "URI:" ).append( action.getURI() ).append( "\n" );

		if ( action instanceof DocumentTargetedAction ) {
			sb.append( "Index: " ).append( ( (DocumentTargetedAction<?>) action ).getIndex() ).append( "\n" );
			sb.append( "Type: " ).append( ( (DocumentTargetedAction<?>) action ).getType() ).append( "\n" );
			sb.append( "Id: " ).append( ( (DocumentTargetedAction<?>) action ).getId() ).append( "\n" );
		}

		sb.append( "Data:\n" );
		sb.append( formatRequestData( gsonService, action ) );
		sb.append( "\n" );
		return sb.toString();
	}

	public static String formatRequestData(GsonService gsonService, Action<?> search) {
		Gson gson = gsonService.getGson();
		/*
		 * The Gson we use as an argument is not always used, which means we have to reformat
		 * the result ourselves to be sure it's pretty-printed.
		 */
		String data = search.getData( gson );

		// Make sure the JSON is pretty-printed
		try {
			gson = gsonService.getGsonPrettyPrinting();
			JsonElement tree = gson.fromJson( data, JsonElement.class );
			return gson.toJson( tree );
		}
		catch (JsonParseException e) {
			/*
			 * Sometimes, for example for bulk requests, the data isn't really JSON,
			 * but rather multiple "\n"-separated JSON documents.
			 * In that case we just give up and return the raw, potentially not
			 * pretty-printed data.
			 */
			return data;
		}
	}

	public static String formatResponse(GsonService gsonService, JestResult result) {
		StringBuilder sb = new StringBuilder();
		sb.append( "Status: " ).append( result.getResponseCode() ).append( "\n" );
		sb.append( "Error message: " ).append( result.getErrorMessage() ).append( "\n" );
		sb.append( "Cluster name: " ).append( property( result, "cluster_name" ) ).append( "\n" );
		sb.append( "Cluster status: " ).append( property( result, "status" ) ).append( "\n" );
		sb.append( "\n" );

		if ( result instanceof BulkResult ) {
			for ( BulkResultItem item : ( (BulkResult) result ).getItems() ) {
				sb.append( "Operation: " ).append( item.operation ).append( "\n" );
				sb.append( "  Index: " ).append( item.index ).append( "\n" );
				sb.append( "  Type: " ).append( item.type ).append( "\n" );
				sb.append( "  Id: " ).append( item.id ).append( "\n" );
				sb.append( "  Status: " ).append( item.status ).append( "\n" );
				sb.append( "  Error: " ).append( item.error ).append( "\n" );
			}
		}

		return sb.toString();
	}

	private static String property(JestResult result, String name) {
		if ( result.getJsonObject() == null ) {
			return null;
		}
		JsonElement propretyValue = result.getJsonObject().get( name );
		if ( propretyValue == null ) {
			return null;
		}
		return propretyValue.getAsString();
	}
}
