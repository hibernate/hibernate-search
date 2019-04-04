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
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.PutIndexMappingWorkBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class PutIndexTypeMappingWork extends AbstractSimpleElasticsearchWork<Void> {

	protected PutIndexTypeMappingWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements PutIndexMappingWorkBuilder {
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
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
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
			// ES6.7 only
			if ( includeTypeName != null ) {
				builder.param( "include_type_name", includeTypeName );
			}
			builder.pathComponent( Paths._MAPPING )
					.body( payload );
			return builder.build();
		}

		@Override
		public PutIndexTypeMappingWork build() {
			return new PutIndexTypeMappingWork( this );
		}
	}
}
