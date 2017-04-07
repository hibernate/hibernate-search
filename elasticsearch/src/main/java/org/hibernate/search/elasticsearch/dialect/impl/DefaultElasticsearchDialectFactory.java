/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import java.io.IOException;
import java.util.Properties;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.dialect.impl.es2.Elasticsearch2Dialect;
import org.hibernate.search.elasticsearch.dialect.impl.es5.Elasticsearch5Dialect;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchDialectFactory implements ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final JsonAccessor VERSION_ACCESSOR = JsonAccessor.root().property( "version" ).property( "number" );

	@Override
	public ElasticsearchDialect createDialect(ElasticsearchClient client, Properties properties) {
		String version;
		try {
			version = getVersion( client );
		}
		catch (RuntimeException e) {
			throw log.failedToDetectElasticsearchVersion( e );
		}

		if ( version.startsWith( "2." ) ) {
			return new Elasticsearch2Dialect();
		}
		else if ( version.startsWith( "5." ) ) {
			return new Elasticsearch5Dialect();
		}
		else {
			throw log.unexpectedElasticsearchVersion( version );
		}
	}

	private String getVersion(ElasticsearchClient client) {
		ElasticsearchRequest request = ElasticsearchRequest.get().build();
		GsonProvider gsonProvider = DialectIndependentGsonProvider.INSTANCE;
		Response response = null;
		JsonObject responseAsJsonObject = null;
		try {
			response = client.execute( request );
			responseAsJsonObject = ElasticsearchClientUtils.parseJsonResponse( gsonProvider, response );

			if ( !ElasticsearchClientUtils.isSuccessCode( response.getStatusLine().getStatusCode() ) ) {
				throw log.elasticsearchRequestFailed(
						ElasticsearchClientUtils.formatRequest( gsonProvider, request ),
						ElasticsearchClientUtils.formatResponse( gsonProvider, response, responseAsJsonObject ),
						null );
			}

			return VERSION_ACCESSOR.get( responseAsJsonObject ).getAsString();
		}
		catch (SearchException e) {
			throw e; // Do not add context for those: we expect SearchExceptions to be self-explanatory
		}
		catch (IOException | RuntimeException e) {
			throw log.elasticsearchRequestFailed(
					ElasticsearchClientUtils.formatRequest( gsonProvider, request ),
					ElasticsearchClientUtils.formatResponse( gsonProvider, response, responseAsJsonObject ),
					e );
		}
	}
}
