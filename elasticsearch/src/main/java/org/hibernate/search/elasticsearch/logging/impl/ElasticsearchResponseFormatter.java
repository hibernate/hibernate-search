/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.logging.impl;

import java.util.Map;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.util.impl.JsonLogHelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link ElasticsearchRequest}s in log messages.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchResponseFormatter {

	private final String stringRepresentation;

	public ElasticsearchResponseFormatter(ElasticsearchResponse response) {
		this.stringRepresentation = formatResponse( response );
	}

	public static String formatResponse(ElasticsearchResponse response) {
		if ( response == null ) {
			return null;
		}

		JsonLogHelper helper = JsonLogHelper.get();

		JsonObject body = response.getBody();
		//Wild guess for some tuning. The only certainty is that the default (16) is too small.
		//Also useful to hint the builder to use larger increment steps.
		StringBuilder sb = new StringBuilder( 180 );
		sb.append( "Status: " ).append( response.getStatusCode() ).append( " " ).append( response.getStatusMessage() );
		sb.append( "\nError message: " ).append( helper.propertyAsString( body, "error" ) );
		sb.append( "\nCluster name: " ).append( helper.propertyAsString( body, "cluster_name" ) );
		sb.append( "\nCluster status: " ).append( helper.propertyAsString( body, "status" ) );
		sb.append( "\n\n" );

		JsonElement items = helper.property( body, "items" );
		if ( items != null && items.isJsonArray() ) {
			for ( JsonElement item : items.getAsJsonArray() ) {
				for ( Map.Entry<String, JsonElement> entry : item.getAsJsonObject().entrySet() ) {
					sb.append( "Operation: " ).append( entry.getKey() );
					JsonElement value = entry.getValue();
					sb.append( "\n  Index: " ).append( helper.propertyAsString( value, "_index" ) );
					sb.append( "\n  Type: " ).append( helper.propertyAsString( value, "_type" ) );
					sb.append( "\n  Id: " ).append( helper.propertyAsString( value, "_id" ) );
					sb.append( "\n  Status: " ).append( helper.propertyAsString( value, "status" ) );
					sb.append( "\n  Error: " ).append( helper.propertyAsString( value, "error" ) );
					sb.append( "\n" );
				}
			}
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
