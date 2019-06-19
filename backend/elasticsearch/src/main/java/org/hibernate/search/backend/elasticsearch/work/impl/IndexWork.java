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
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;

import com.google.gson.JsonObject;


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
			extends AbstractSimpleBulkableElasticsearchWork.AbstractBuilder<Builder>
			implements IndexWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final String routingKey;
		private final JsonObject document;

		public static Builder forElasticsearch67AndBelow(URLEncodedString indexName, URLEncodedString typeName,
				URLEncodedString id, String routingKey, JsonObject document) {
			return new Builder( indexName, typeName, id, routingKey, document );
		}

		public static Builder forElasticsearch7AndAbove(URLEncodedString indexName,
				URLEncodedString id, String routingKey, JsonObject document) {
			return new Builder( indexName, null, id, routingKey, document );
		}

		private Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String routingKey, JsonObject document) {
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
					.pathComponent( typeName != null ? typeName : Paths._DOC ) // _doc for ES7+
					.pathComponent( id )
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
			if ( typeName != null ) { // ES6.x and below only
				index.addProperty( "_type", typeName.original );
			}
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