/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.stub;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientImplementor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.util.StringHelper;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class BlackholeElasticsearchClient implements ElasticsearchClientImplementor {

	private final ElasticsearchResponse getResponse;
	private final ElasticsearchResponse okResponse;

	public BlackholeElasticsearchClient(String elasticsearchVersion) {
		this.getResponse = new ElasticsearchResponse(
				200, "OK",
				JsonBuilder.object()
						.add( "version", JsonBuilder.object()
								.addProperty( "number", elasticsearchVersion )
						)
						.build()
				);
		this.okResponse = new ElasticsearchResponse( 200, "OK", new JsonObject() );
	}

	@Override
	public void init(GsonProvider gsonProvider) {
		// Nothing to do
	}

	@Override
	public void close() throws IOException {
		// Nothing to do
	}

	@Override
	public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request) {
		return CompletableFuture.completedFuture( generateResponse( request ) );
	}

	public ElasticsearchResponse generateResponse(ElasticsearchRequest request) {
		String method = request.getMethod();
		String path = request.getPath();

		if ( "GET".equals( method ) && ( StringHelper.isEmpty( path ) || "/".equals( path ) ) ) {
			return getResponse;
		}
		else if ( "POST".equals( method ) && path.endsWith( "/_bulk/" ) ) {
			JsonBuilder.Array builder = JsonBuilder.array();

			// In our case we only bulk requests without a body
			int itemCount = request.getBodyParts().size();
			for ( int i = 0 ; i < itemCount ; ++i ) {
				builder.add( JsonBuilder.object()
						.add( "foo", JsonBuilder.object()
								.addProperty( "status", 200 )
						) );
			}

			JsonObject responseJson = JsonBuilder.object()
					.add( "items", builder )
					.build();
			return new ElasticsearchResponse( 200, "OK", responseJson );
		}
		else {
			return okResponse;
		}
	}

}
