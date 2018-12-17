/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.real.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.real.accessor.impl.DefaultElasticsearchRequestSuccessAssessor;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class IndexWork extends AbstractSimpleBulkableElasticsearchWork<Void> {

	public IndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		return null;
	}

	public static class Builder
			extends AbstractSimpleBulkableElasticsearchWork.Builder<Builder>
			implements IndexWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final String routingKey;
		private final JsonObject document;

		public Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String routingKey, JsonObject document) {
			super( indexName, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.typeName = typeName;
			this.id = id;
			this.routingKey = routingKey;
			this.document = document;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.put()
					.pathComponent( indexName )
					.pathComponent( typeName )
					.pathComponent( id )

					// TODO avoid this param using a smart orchestrator
					.param( "refresh", true )

					.body( document );

			if ( routingKey != null ) {
				builder.param( "routing", routingKey );
			}

			return builder.build();
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			JsonObject index = new JsonObject();
			index.addProperty( "_index", indexName.original );
			index.addProperty( "_type", typeName.original );
			index.addProperty( "_id", id.original );

			JsonObject result = new JsonObject();
			result.add( "index", index );

			return result;
		}

		@Override
		public IndexWork build() {
			return new IndexWork( this );
		}
	}
}