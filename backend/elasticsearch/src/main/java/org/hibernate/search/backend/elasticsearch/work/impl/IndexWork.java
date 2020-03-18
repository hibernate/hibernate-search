/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;

import com.google.gson.JsonObject;


public class IndexWork extends AbstractSingleDocumentElasticsearchWork<Void>
		implements SingleDocumentElasticsearchWork<Void> {

	private IndexWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		return null;
	}

	public static class Builder
			extends AbstractSingleDocumentElasticsearchWork.AbstractBuilder<Builder>
			implements IndexWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final String routingKey;
		private final JsonObject document;

		public static Builder forElasticsearch67AndBelow(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName, URLEncodedString typeName, URLEncodedString id, String routingKey,
				JsonObject document) {
			return new Builder( entityTypeName, entityIdentifier,
					elasticsearchIndexName, typeName, id, routingKey, document );
		}

		public static Builder forElasticsearch7AndAbove(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName, URLEncodedString id, String routingKey,
				JsonObject document) {
			return new Builder( entityTypeName, entityIdentifier,
					elasticsearchIndexName, null, id, routingKey, document );
		}

		private Builder(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
					URLEncodedString typeName, URLEncodedString id, String routingKey, JsonObject document) {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE, entityTypeName, entityIdentifier );
			this.indexName = elasticsearchIndexName;
			this.typeName = typeName;
			this.id = id;
			this.routingKey = routingKey;
			this.document = document;
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			JsonObject index = new JsonObject();
			index.addProperty( "_index", indexName.original );
			if ( typeName != null ) { // ES6.x and below only
				index.addProperty( "_type", typeName.original );
			}

			index.addProperty( "_id", id.original );

			if ( routingKey != null ) {
				index.addProperty( "routing", routingKey );
			}

			JsonObject result = new JsonObject();
			result.add( "index", index );

			return result;
		}

		@Override
		protected JsonObject buildBulkableActionBody() {
			return document;
		}

		@Override
		public IndexWork build() {
			return new IndexWork( this );
		}
	}
}