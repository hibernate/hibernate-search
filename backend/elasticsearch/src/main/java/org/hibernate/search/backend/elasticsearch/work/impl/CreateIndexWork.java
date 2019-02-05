/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.CreateIndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class CreateIndexWork extends AbstractSimpleElasticsearchWork<CreateIndexResult> {

	protected CreateIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected CreateIndexResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		int statusCode = response.getStatusCode();
		if ( ElasticsearchClientUtils.isSuccessCode( statusCode ) ) {
			return CreateIndexResult.CREATED;
		}
		else {
			// Can only happen if ignoreExisting() was called on the builder
			return CreateIndexResult.ALREADY_EXISTS;
		}
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements CreateIndexWorkBuilder {
		private final GsonProvider gsonProvider;
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private JsonObject payload = new JsonObject();

		public Builder(GsonProvider gsonProvider, URLEncodedString indexName, URLEncodedString typeName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.gsonProvider = gsonProvider;
			this.indexName = indexName;
			this.typeName = typeName;
		}

		@Override
		public Builder settings(IndexSettings settings) {
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();
			payload.add( "settings", gson.toJsonTree( settings ) );
			return this;
		}

		@Override
		public Builder mapping(RootTypeMapping mapping) {
			Gson gson = gsonProvider.getGsonNoSerializeNulls();

			JsonObject mappings = payload.getAsJsonObject( "mappings" );
			if ( mappings == null ) {
				mappings = new JsonObject();
				payload.add( "mappings", mappings );
			}

			mappings.add( typeName.original, gson.toJsonTree( mapping ) );

			return this;
		}

		@Override
		public Builder ignoreExisting() {
			this.resultAssessor = DefaultElasticsearchRequestSuccessAssessor.builder()
					.ignoreErrorTypes( "index_already_exists_exception" )
					.build();
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.put()
					.pathComponent( indexName );

			if ( payload.size() > 0 ) {
				builder.body( payload );
			}
			return builder.build();
		}

		@Override
		public CreateIndexWork build() {
			return new CreateIndexWork( this );
		}
	}
}