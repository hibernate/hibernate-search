/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;


public class ElasticsearchClientUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> VERSION_ACCESSOR =
			JsonAccessor.root().property( "version" ).property( "number" ).asString();

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) throws IOException {
		final List<JsonObject> bodyParts = request.getBodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		return new GsonHttpEntity( gson, bodyParts );
	}

	public static ElasticsearchVersion getElasticsearchVersion(ElasticsearchClient client) {
		try {
			ElasticsearchRequest request = ElasticsearchRequest.get().build();
			ElasticsearchResponse response = null;
			try {
				response = client.submit( request ).join();

				if ( !ElasticsearchClientUtils.isSuccessCode( response.getStatusCode() ) ) {
					throw log.elasticsearchResponseIndicatesFailure();
				}

				return VERSION_ACCESSOR.get( response.getBody() )
						.map( ElasticsearchVersion::of )
						.orElseThrow( () -> new AssertionFailure( "Missing version number in JSON response" ) );
			}
			catch (RuntimeException e) {
				throw log.elasticsearchRequestFailed( request, response, e );
			}
		}
		catch (RuntimeException e) {
			throw log.failedToDetectElasticsearchVersion( e );
		}
	}

}
