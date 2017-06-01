/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.elasticsearch.work.impl.builder.CreateIndexWorkBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class CreateIndexWork extends SimpleElasticsearchWork<CreateIndexResult> {

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
			extends SimpleElasticsearchWork.Builder<Builder>
			implements CreateIndexWorkBuilder {
		private final GsonProvider gsonProvider;
		private final URLEncodedString indexName;
		private JsonObject payload;

		public Builder(GsonProvider gsonProvider, URLEncodedString indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.gsonProvider = gsonProvider;
			this.indexName = indexName;
		}

		@Override
		public Builder settings(IndexSettings settings) {
			if ( settings != null ) {
				/*
				 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
				 * We better not include the null fields.
				 */
				Gson gson = gsonProvider.getGsonNoSerializeNulls();
				this.payload = gson.toJsonTree( settings ).getAsJsonObject();
			}
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

			if ( payload != null ) {
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