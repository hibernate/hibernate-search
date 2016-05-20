/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch;

import org.hibernate.search.elasticsearch.impl.ElasticsearchJsonQueryDescriptor;
import org.hibernate.search.elasticsearch.impl.GsonHolder;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.query.engine.spi.QueryDescriptor;

/**
 * Creates queries to be used with the Elasticsearch backend.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchQueries {

	private ElasticsearchQueries() {
	}

	/**
	 * Creates an Elasticsearch query from the given JSON query representation. See the <a
	 * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html">official
	 * documentation</a> for the complete query syntax.
	 */
	public static QueryDescriptor fromJson(String jsonQuery) {
		return new ElasticsearchJsonQueryDescriptor( GsonHolder.PARSER.parse( jsonQuery ).getAsJsonObject() );
	}

	/**
	 * Creates an Elasticsearch query from the given Query String Query, as e.g. to be used with the "q" parameter in
	 * the Elasticsearch API. See the <a
	 * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html">official
	 * documentation</a> for a description of the query syntax.
	 */
	public static QueryDescriptor fromQueryString(String queryStringQuery) {
		// Payload looks like so:
		// { "query" : { "query_string" : { "query" : "abstract:Hibernate" } } }

		JsonBuilder.Object query = JsonBuilder.object().add( "query",
				JsonBuilder.object().add( "queryString",
						JsonBuilder.object().addProperty( "query", queryStringQuery ) ) );

		return new ElasticsearchJsonQueryDescriptor( query.build() );
	}
}
