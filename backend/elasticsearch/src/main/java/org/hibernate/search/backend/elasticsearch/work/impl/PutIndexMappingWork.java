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

	public static class Builder extends AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final JsonObject payload;

		public static Builder create(GsonProvider gsonProvider,
				URLEncodedString indexName, RootTypeMapping typeMapping) {
			return new Builder( gsonProvider, indexName, typeMapping );
		}

		private Builder(
				GsonProvider gsonProvider,
				URLEncodedString indexName,
				RootTypeMapping typeMapping) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonProvider.getGsonNoSerializeNulls();
			this.payload = gson.toJsonTree( typeMapping ).getAsJsonObject();
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder = ElasticsearchRequest.put().pathComponent( indexName );
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
