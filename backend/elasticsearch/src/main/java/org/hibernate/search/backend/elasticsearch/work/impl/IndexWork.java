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


public class IndexWork extends AbstractSingleDocumentIndexingWork
		implements SingleDocumentIndexingWork {

	private IndexWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends AbstractSingleDocumentIndexingWork.AbstractBuilder<Builder>
			implements IndexWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final String documentIdentifier;
		private final String routingKey;
		private final JsonObject document;

		public static Builder forElasticsearch67AndBelow(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName, URLEncodedString typeName,
				String documentIdentifier, String routingKey,
				JsonObject document) {
			return new Builder( entityTypeName, entityIdentifier,
					elasticsearchIndexName, typeName, documentIdentifier, routingKey, document );
		}

		public static Builder forElasticsearch7AndAbove(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName, String documentIdentifier, String routingKey,
				JsonObject document) {
			return new Builder( entityTypeName, entityIdentifier,
					elasticsearchIndexName, null, documentIdentifier, routingKey, document );
		}

		private Builder(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
					URLEncodedString typeName, String documentIdentifier, String routingKey, JsonObject document) {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE, entityTypeName, entityIdentifier );
			this.indexName = elasticsearchIndexName;
			this.typeName = typeName;
			this.documentIdentifier = documentIdentifier;
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

			index.addProperty( "_id", documentIdentifier );

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