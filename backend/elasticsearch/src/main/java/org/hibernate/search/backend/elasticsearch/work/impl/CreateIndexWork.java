/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CreateIndexWork extends AbstractNonBulkableWork<CreateIndexResult> {

	private static final String MAPPINGS_PROPERTY = "mappings";

	protected CreateIndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected CreateIndexResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		int statusCode = response.statusCode();
		if ( ElasticsearchClientUtils.isSuccessCode( statusCode ) ) {
			return CreateIndexResult.CREATED;
		}
		else {
			// Can only happen if ignoreExisting() was called on the builder
			return CreateIndexResult.ALREADY_EXISTS;
		}
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final GsonProvider gsonProvider;
		private final URLEncodedString indexName;
		private final JsonObject payload = new JsonObject();

		public static Builder create(GsonProvider gsonProvider, URLEncodedString indexName) {
			return new Builder( gsonProvider, indexName );
		}

		private Builder(GsonProvider gsonProvider, URLEncodedString indexName) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.gsonProvider = gsonProvider;
			this.indexName = indexName;
		}

		public Builder aliases(Map<String, IndexAliasDefinition> aliases) {
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();
			payload.add( "aliases", gson.toJsonTree( aliases ) );
			return this;
		}

		public Builder settings(IndexSettings settings) {
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();
			payload.add( "settings", gson.toJsonTree( settings ) );
			return this;
		}

		public Builder mapping(RootTypeMapping mapping) {
			Gson gson = gsonProvider.getGsonNoSerializeNulls();

			payload.add( MAPPINGS_PROPERTY, gson.toJsonTree( mapping ) );

			return this;
		}

		public Builder ignoreExisting() {
			this.resultAssessor = ElasticsearchRequestSuccessAssessor.builder()
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
