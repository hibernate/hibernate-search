/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.Gson;
import com.google.gson.JsonObject;


public class PutIndexMappingWork extends AbstractNonBulkableWork<Void> {

	protected PutIndexMappingWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final Boolean includeTypeName;
		private final JsonObject payload;

		public static Builder forElasticsearch66AndBelow(GsonProvider gsonProvider,
				URLEncodedString indexName, URLEncodedString typeName, RootTypeMapping typeMapping) {
			return new Builder( gsonProvider, indexName, typeName, null, typeMapping );
		}

		public static Builder forElasticsearch67(GsonProvider gsonProvider,
				URLEncodedString indexName, URLEncodedString typeName, RootTypeMapping typeMapping) {
			/*
			 * Pushing the mapping with a type name will trigger a warning,
			 * but that's the only way to keep the index similar to what it was in 6.6,
			 * i.e. to avoid changing the index when a user migrates from 6.6 to 6.7.
			 * So we'll accept this single warning for now.
			 */
			return new Builder( gsonProvider, indexName, typeName, true, typeMapping );
		}

		public static Builder forElasticsearch7AndAbove(GsonProvider gsonProvider,
				URLEncodedString indexName, RootTypeMapping typeMapping) {
			return new Builder( gsonProvider, indexName, null, null, typeMapping );
		}

		private Builder(
				GsonProvider gsonProvider,
				URLEncodedString indexName, URLEncodedString typeName, Boolean includeTypeName,
				RootTypeMapping typeMapping) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
			this.typeName = typeName;
			this.includeTypeName = includeTypeName;
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();
			this.payload = gson.toJsonTree( typeMapping ).getAsJsonObject();
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.put()
					.pathComponent( indexName );
			// ES6.6 and below only
			if ( typeName != null ) {
				builder.pathComponent( typeName );
			}
			// ES6.7 and later 6.x only
			if ( includeTypeName != null ) {
				builder.param( "include_type_name", includeTypeName );
			}
			builder.pathComponent( Paths._MAPPING )
					.body( payload );
			return builder.build();
		}

		@Override
		public PutIndexMappingWork build() {
			return new PutIndexMappingWork( this );
		}
	}
}
