/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;

public class IndexWork extends AbstractSingleDocumentIndexingWork
		implements SingleDocumentIndexingWork {

	private IndexWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends AbstractSingleDocumentIndexingWork.AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final String routingKey;
		private final JsonObject document;

		public static Builder create(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName, String documentIdentifier, String routingKey,
				JsonObject document) {
			return new Builder( entityTypeName, entityIdentifier,
					elasticsearchIndexName, documentIdentifier, routingKey, document );
		}

		private Builder(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
				String documentIdentifier, String routingKey, JsonObject document) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE, entityTypeName, entityIdentifier,
					documentIdentifier );
			this.indexName = elasticsearchIndexName;
			this.routingKey = routingKey;
			this.document = document;
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			JsonObject index = new JsonObject();
			index.addProperty( "_index", indexName.original );
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
