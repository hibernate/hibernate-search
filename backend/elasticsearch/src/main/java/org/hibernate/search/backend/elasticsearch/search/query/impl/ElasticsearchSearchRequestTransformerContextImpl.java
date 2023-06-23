/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformerContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

import com.google.gson.JsonObject;

final class ElasticsearchSearchRequestTransformerContextImpl
		implements ElasticsearchSearchRequestTransformerContext {

	static Function<ElasticsearchRequest, ElasticsearchRequest> createTransformerFunction(
			ElasticsearchSearchRequestTransformer transformer) {
		if ( transformer == null ) {
			return null;
		}
		return request -> new ElasticsearchSearchRequestTransformerContextImpl( request ).apply( transformer );
	}

	private final ElasticsearchRequest originalRequest;
	private final JsonObject originalBody;

	private String path;
	private JsonObject potentiallyTransformedBody;
	private Map<String, String> potentiallyTransformedParametersMap;

	private ElasticsearchSearchRequestTransformerContextImpl(ElasticsearchRequest originalRequest) {
		this.originalRequest = originalRequest;
		List<JsonObject> originalBodyParts = originalRequest.bodyParts();
		if ( originalBodyParts.size() != 1 ) {
			throw new AssertionFailure(
					"Request transformation was applied to a request with no body part or more than one body parts."
			);
		}
		this.originalBody = originalBodyParts.get( 0 );
		this.path = originalRequest.path();
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public void path(String newPath) {
		Contracts.assertNotNullNorEmpty( newPath, "newPath" );
		this.path = newPath;
	}

	@Override
	public Map<String, String> parametersMap() {
		// Avoid side-effects on the original request
		if ( potentiallyTransformedParametersMap == null ) {
			potentiallyTransformedParametersMap = new LinkedHashMap<>( originalRequest.parameters() );
		}
		return potentiallyTransformedParametersMap;
	}

	@Override
	public JsonObject body() {
		// Avoid side-effects on the original request
		if ( potentiallyTransformedBody == null ) {
			potentiallyTransformedBody = originalBody.deepCopy();
		}
		return potentiallyTransformedBody;
	}

	public ElasticsearchRequest apply(ElasticsearchSearchRequestTransformer transformer) {
		transformer.transform( this );

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.builder( originalRequest.method() );

		builder.wholeEncodedPath( path );

		Map<String, String> parameters = potentiallyTransformedParametersMap != null
				? potentiallyTransformedParametersMap
				: originalRequest.parameters();
		parameters.forEach( builder::param );

		JsonObject body = potentiallyTransformedBody != null ? potentiallyTransformedBody : originalBody;
		if ( body != null ) {
			builder.body( body );
		}

		return builder.build();
	}
}
