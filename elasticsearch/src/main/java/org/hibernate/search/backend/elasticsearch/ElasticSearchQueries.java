/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch;

import org.hibernate.search.backend.elasticsearch.impl.ElasticSearchHSQueryImpl;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.QueryDescriptor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Creates queries to be used with the ElasticSearch backend.
 *
 * @author Gunnar Morling
 */
public class ElasticSearchQueries {

	private ElasticSearchQueries() {
	}

	/**
	 * Creates an ElasticSearch query from the given JSON query representation. See the <a
	 * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html">official
	 * documentation</a> for the complete query syntax.
	 */
	public static QueryDescriptor fromJson(String jsonQuery) {
		// TODO Parse + Re-render using Gson for now to leverage single quote support
		jsonQuery = new JsonParser().parse( jsonQuery ).toString();

		return new ElasticSearchJsonQuery( jsonQuery );
	}

	/**
	 * Creates an ElasticSearch query from the given Query String Query, as e.g. to be used with the "q" parameter in
	 * the ElasticSearch API. See the <a
	 * href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html">official
	 * documentation</a> for a description of the query syntax.
	 */
	public static QueryDescriptor fromQueryString(String queryStringQuery) {
		// Payload looks like so:
		// { "query" : { "query_string" : { "query" : "abstract:Hibernate" } } }

		JsonObject query = new JsonObject();
		query.addProperty( "query", queryStringQuery );

		JsonObject queryString = new JsonObject();
		queryString.add( "query_string", query );

		JsonObject queryObject = new JsonObject();
		queryObject.add( "query", queryString );

		return new ElasticSearchJsonQuery( queryObject.toString() );
	}

	private static class ElasticSearchJsonQuery implements QueryDescriptor {

		private final String jsonQuery;

		public ElasticSearchJsonQuery(String jsonQuery) {
			this.jsonQuery = jsonQuery;
		}

		@Override
		public HSQuery createHSQuery(ExtendedSearchIntegrator extendedIntegrator) {
			return new ElasticSearchHSQueryImpl( jsonQuery, extendedIntegrator );
		}

		@Override
		public String toString() {
			return jsonQuery;
		}
	}
}
