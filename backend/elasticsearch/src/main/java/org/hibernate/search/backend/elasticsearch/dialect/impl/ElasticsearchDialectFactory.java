/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.dialect.impl.es56.Elasticsearch56Dialect;
import org.hibernate.search.backend.elasticsearch.dialect.impl.es6.Elasticsearch6Dialect;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Allows to create an Elasticsearch dialect by detecting the version of a remote cluster.
 */
public class ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> VERSION_ACCESSOR =
			JsonAccessor.root().property( "version" ).property( "number" ).asString();

	public ElasticsearchDialect createFromClusterVersion(ElasticsearchClient client) {
		String version;
		try {
			version = getVersion( client );
		}
		catch (RuntimeException e) {
			throw log.failedToDetectElasticsearchVersion( e );
		}

		if ( version.startsWith( "0." ) || version.startsWith( "1." ) || version.startsWith( "2." ) ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( version.startsWith( "5." ) ) {
			if ( version.startsWith( "5.0." ) || version.startsWith( "5.1." )
					|| version.startsWith( "5.2." ) || version.startsWith( "5.3." )
					|| version.startsWith( "5.4." ) || version.startsWith( "5.5." ) ) {
				throw log.unsupportedElasticsearchVersion( version );
			}
			else {
				return new Elasticsearch56Dialect();
			}
		}
		else {
			// Either the latest supported version, or a newer/unknown one
			if ( !version.startsWith( "6." ) ) {
				log.unexpectedElasticsearchVersion( version );
			}
			return new Elasticsearch6Dialect();
		}
	}

	private String getVersion(ElasticsearchClient client) {
		ElasticsearchRequest request = ElasticsearchRequest.get().build();
		ElasticsearchResponse response = null;
		try {
			response = client.submit( request ).join();

			if ( !ElasticsearchClientUtils.isSuccessCode( response.getStatusCode() ) ) {
				throw log.elasticsearchResponseIndicatesFailure();
			}

			return VERSION_ACCESSOR.get( response.getBody() )
					.orElseThrow( () -> new AssertionFailure( "Missing version number in JSON response" ) );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchRequestFailed( request, response, e );
		}
	}
}
