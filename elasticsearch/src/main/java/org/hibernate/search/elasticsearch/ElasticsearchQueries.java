/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.elasticsearch.impl.ElasticsearchJsonQueryDescriptor;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Creates queries to be used with Elasticsearch.
 *
 * <p>Methods in this class return {@link QueryDescriptor}s. See {@code QueryDescriptor}'s
 * javadoc for more information about how to use it.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchQueries {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final JsonParser PARSER = new JsonParser();

	private static final Set<String> ALLOWED_PAYLOAD_ATTRIBUTES = Collections.unmodifiableSet(
			CollectionHelper.asSet( "query" ) );

	private ElasticsearchQueries() {
	}

	/**
	 * Creates an Elasticsearch query from the given JSON payload for the Elasticsearch Search API.
	 * <p>
	 * Note that only the 'query' attribute is supported.
	 * <p>
	 * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html">
	 * official documentation</a> for the complete payload syntax.
	 *
	 * @param payload The JSON payload as a String
	 * @return A query descriptor that can be used to create a query.
	 */
	public static QueryDescriptor fromJson(String payload) {
		JsonObject payloadAsJsonObject;

		try {
			payloadAsJsonObject = PARSER.parse( payload ).getAsJsonObject();
		}
		catch (IllegalStateException | JsonSyntaxException e) {
			throw LOG.invalidSearchAPIPayload( e );
		}

		List<String> invalidAttributes = new ArrayList<>();
		for ( Map.Entry<String, ?> entry : payloadAsJsonObject.entrySet() ) {
			String payloadAttribute = entry.getKey();
			if ( ! ALLOWED_PAYLOAD_ATTRIBUTES.contains( payloadAttribute ) ) {
				invalidAttributes.add( payloadAttribute );
			}
		}
		if ( !invalidAttributes.isEmpty() ) {
			throw LOG.unsupportedSearchAPIPayloadAttributes( invalidAttributes );
		}

		return new ElasticsearchJsonQueryDescriptor( PARSER.parse( payload ).getAsJsonObject() );
	}

	/**
	 * Creates an Elasticsearch query from the given Query String Query, as e.g. to be used with the "q" parameter in
	 * the Elasticsearch Search API.
	 * <p>
	 * See the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html">
	 * official documentation</a> for the query syntax.
	 *
	 * @param queryStringQuery A query string conforming to the "query string" syntax.
	 * @return A query descriptor that can be used to create a query.
	 */
	public static QueryDescriptor fromQueryString(String queryStringQuery) {
		// Payload looks like so:
		// { "query" : { "query_string" : { "query" : "abstract:Hibernate" } } }

		JsonBuilder.Object query = JsonBuilder.object().add( "query",
				JsonBuilder.object().add( "query_string",
						JsonBuilder.object().addProperty( "query", queryStringQuery ) ) );

		return new ElasticsearchJsonQueryDescriptor( query.build() );
	}
}
