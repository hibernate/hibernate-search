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

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;

public class ElasticsearchClientUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> DISTRIBUTION_ACCESSOR =
			JsonAccessor.root().property( "version" ).property( "distribution" ).asString();
	private static final JsonAccessor<String> VERSION_ACCESSOR =
			JsonAccessor.root().property( "version" ).property( "number" ).asString();

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) throws IOException {
		final List<JsonObject> bodyParts = request.bodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		return new GsonHttpEntity( gson, bodyParts );
	}

	public static ElasticsearchVersion tryGetElasticsearchVersion(ElasticsearchClient client) {
		ElasticsearchRequest request = ElasticsearchRequest.get().build();
		ElasticsearchResponse response = null;
		try {
			response = Futures.unwrappedExceptionJoin( client.submit( request ) );

			if ( response.statusCode() == 404 ) {
				return null;
			}

			if ( !ElasticsearchClientUtils.isSuccessCode( response.statusCode() ) ) {
				throw log.elasticsearchResponseIndicatesFailure();
			}

			ElasticsearchDistributionName distributionOptional = DISTRIBUTION_ACCESSOR.get( response.body() )
					.map( ElasticsearchDistributionName::fromServerResponseRepresentation )
					// Only the Elastic distribution doesn't mention what it is.
					.orElse( ElasticsearchDistributionName.ELASTIC );
			String version = VERSION_ACCESSOR.get( response.body() )
					.orElseThrow( () -> new AssertionFailure( "Missing version number in JSON response" ) );

			return ElasticsearchVersion.of( distributionOptional, version );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchRequestFailed( request, response, e.getMessage(), e );
		}
	}

}
