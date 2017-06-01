/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.work.impl.builder.PutIndexMappingWorkBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class PutIndexTypeMappingWork extends SimpleElasticsearchWork<Void> {

	protected PutIndexTypeMappingWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements PutIndexMappingWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final JsonObject payload;

		public Builder(
				GsonProvider gsonProvider,
				URLEncodedString indexName, URLEncodedString typeName, TypeMapping typeMapping) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.typeName = typeName;
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
					.pathComponent( indexName )
					.pathComponent( typeName )
					.pathComponent( Paths._MAPPING )
					.body( payload );
			return builder.build();
		}

		@Override
		public PutIndexTypeMappingWork build() {
			return new PutIndexTypeMappingWork( this );
		}
	}
}