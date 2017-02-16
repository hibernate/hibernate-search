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

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.indices.mapping.GetMapping;

public class GetIndexTypeMappingsWork extends SimpleElasticsearchWork<JestResult, Map<String, TypeMapping>> {

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
	protected Map<String, TypeMapping> generateResult(ElasticsearchWorkExecutionContext context, JestResult response) {
		JsonObject resultJson = response.getJsonObject();
		JsonElement index = response.getJsonObject().get( indexName );
		if ( index == null || !index.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + resultJson );
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );

		if ( mappings != null ) {
			GsonService gsonService = context.getGsonService();
			Type mapType = STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN.getType();
			return gsonService.getGson().<Map<String, TypeMapping>>fromJson( mappings, mapType );
		}
		else {
			return Collections.emptyMap();
		}
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder, JestResult> {
		private final String indexName;
		private final GetMapping.Builder jestBuilder;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
			this.indexName = indexName;
			this.jestBuilder = new GetMapping.Builder().addIndex( indexName );
		}

		@Override
		protected Action<JestResult> buildAction() {
			return jestBuilder.build();
		}

		@Override
		public GetIndexTypeMappingsWork build() {
			return new GetIndexTypeMappingsWork( this );
		}
	}
}