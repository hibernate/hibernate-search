/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.reflect.Type;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.GetIndexTypeMappingWorkBuilder;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class GetIndexTypeMappingWork extends AbstractSimpleElasticsearchWork<RootTypeMapping> {

	private static final TypeToken<Map<String, RootTypeMapping>> STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, RootTypeMapping>>() {
				// Create a new class to capture generic parameters
			};

	private final URLEncodedString indexName;
	private final URLEncodedString typeName;

	protected GetIndexTypeMappingWork(Builder builder) {
		super( builder );
		this.indexName = builder.indexName;
		this.typeName = builder.typeName;
	}

	@Override
	protected RootTypeMapping generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		JsonElement index = body.get( indexName.original );
		if ( index == null || !index.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + body );
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );

		if ( mappings != null ) {
			GsonProvider gsonProvider = context.getGsonProvider();
			if ( typeName != null ) {
				// ES6 and below
				Type mapType = STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN.getType();
				Map<String, RootTypeMapping> mappingsMap = gsonProvider.getGson().fromJson( mappings, mapType );
				return mappingsMap.get( typeName.original );
			}
			else {
				// ES7 and above
				return gsonProvider.getGson().fromJson( mappings, RootTypeMapping.class );
			}
		}
		else {
			return null;
		}
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements GetIndexTypeMappingWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final Boolean includeTypeName;

		public static Builder forElasticsearch66AndBelow(URLEncodedString indexName, URLEncodedString typeName) {
			return new Builder( indexName, typeName, null );
		}

		public static Builder forElasticsearch67(URLEncodedString indexName, URLEncodedString typeName) {
			return new Builder( indexName, typeName, true );
		}

		public static Builder forElasticsearch7AndAbove(URLEncodedString indexName) {
			return new Builder( indexName, null, null );
		}

		private Builder(URLEncodedString indexName, URLEncodedString typeName, Boolean includeTypeName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.typeName = typeName;
			this.includeTypeName = includeTypeName;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( indexName )
					.pathComponent( Paths._MAPPING );
			// ES6.7 and later 6.x only
			if ( includeTypeName != null ) {
				builder.param( "include_type_name", includeTypeName );
			}
			return builder.build();
		}

		@Override
		public GetIndexTypeMappingWork build() {
			return new GetIndexTypeMappingWork( this );
		}
	}
}
