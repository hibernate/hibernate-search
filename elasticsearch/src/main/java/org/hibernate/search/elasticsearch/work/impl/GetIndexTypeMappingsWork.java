/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexTypeMappingsWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class GetIndexTypeMappingsWork extends SimpleElasticsearchWork<Map<String, TypeMapping>> {

	private static final TypeToken<Map<String, TypeMapping>> STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, TypeMapping>>() {
				// Create a new class to capture generic parameters
			};

	private final String indexName;

	protected GetIndexTypeMappingsWork(Builder builder) {
		super( builder );
		this.indexName = builder.indexName;
	}

	@Override
	protected Map<String, TypeMapping> generateResult(ElasticsearchWorkExecutionContext context,
			Response response, JsonObject parsedResponseBody) {
		JsonElement index = parsedResponseBody.get( indexName );
		if ( index == null || !index.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + parsedResponseBody );
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );

		if ( mappings != null ) {
			GsonProvider gsonProvider = context.getGsonProvider();
			Type mapType = STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN.getType();
			return gsonProvider.getGson().<Map<String, TypeMapping>>fromJson( mappings, mapType );
		}
		else {
			return Collections.emptyMap();
		}
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements GetIndexTypeMappingsWorkBuilder {
		private final String indexName;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( indexName )
					.pathComponent( "_mapping" );
			return builder.build();
		}

		@Override
		public GetIndexTypeMappingsWork build() {
			return new GetIndexTypeMappingsWork( this );
		}
	}
}