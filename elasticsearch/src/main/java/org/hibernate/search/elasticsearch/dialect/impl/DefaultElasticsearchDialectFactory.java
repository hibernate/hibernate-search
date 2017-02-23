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
import org.elasticsearch.client.RestClient;
import org.hibernate.search.elasticsearch.dialect.impl.es2.Elasticsearch2Dialect;
import org.hibernate.search.elasticsearch.dialect.impl.es5.Elasticsearch5Dialect;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchDialectFactory implements ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final JsonAccessor VERSION_ACCESSOR = JsonAccessor.root().property( "version" ).property( "number" );

	@Override
	public ElasticsearchDialect createDialect(RestClient client, Properties properties) {
		JsonObject responseAsJsonObject;
		try {
			Response response = client.performRequest( "GET", "/" );
			responseAsJsonObject = ElasticsearchClientUtils.parseJsonResponse(
					DialectIndependentGsonProvider.INSTANCE, response );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToDetectElasticsearchVersion( e );
		}

		String version = VERSION_ACCESSOR.get( responseAsJsonObject ).getAsString();

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
}
