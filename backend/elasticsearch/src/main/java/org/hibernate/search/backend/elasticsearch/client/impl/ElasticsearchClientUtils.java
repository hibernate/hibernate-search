package org.hibernate.search.backend.elasticsearch.client.impl;


import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

public final class ElasticsearchClientUtils {

	private static final JsonAccessor<String> DISTRIBUTION_ACCESSOR =
			JsonAccessor.root().property( "version" ).unwrapSingletonArrays().property( "distribution" ).unwrapSingletonArrays()
					.asString();
	private static final JsonAccessor<String> VERSION_ACCESSOR =
			JsonAccessor.root().property( "version" ).unwrapSingletonArrays().property( "number" ).unwrapSingletonArrays()
					.asString();

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

	public static ElasticsearchVersion tryGetElasticsearchVersion(ElasticsearchJsonClient client) {
		ElasticsearchJsonRequest request = ElasticsearchJsonRequest.get().build();
		ElasticsearchJsonResponse response = null;
		try {
			response = Futures.unwrappedExceptionJoin( client.submit( request ) );

			if ( response.statusCode() == 404 ) {
				return null;
			}

			if ( !ElasticsearchClientUtils.isSuccessCode( response.statusCode() ) ) {
				throw ElasticsearchClientLog.INSTANCE.elasticsearchResponseIndicatesFailure();
			}

			ElasticsearchDistributionName distributionOptional = DISTRIBUTION_ACCESSOR.get( response.bodyJson() )
					.map( ElasticsearchDistributionName::fromServerResponseRepresentation )
					// Only the Elastic distribution doesn't mention what it is.
					.orElse( ElasticsearchDistributionName.ELASTIC );
			String version = VERSION_ACCESSOR.get( response.bodyJson() )
					.orElseThrow( () -> new AssertionFailure( "Missing version number in JSON response" ) );

			return ElasticsearchVersion.of( distributionOptional, version );
		}
		catch (RuntimeException e) {
			throw ElasticsearchClientLog.INSTANCE.elasticsearchRequestFailed(
					new org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchRequestFormatter( request )
							.toString(),
					new org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchResponseFormatter( response )
							.toString(),
					e.getMessage(), e );
		}
	}

}
