/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.JsonObject;

public class DeleteWork extends AbstractSingleDocumentIndexingWork {

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			ElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	private DeleteWork(Builder builder) {
		super( builder );
	}

	public static class Builder
			extends AbstractSingleDocumentIndexingWork.AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final String routingKey;

		public static Builder create(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName, String documentIdentifier, String routingKey) {
			return new Builder( entityTypeName, entityIdentifier,
					elasticsearchIndexName, documentIdentifier, routingKey
			);
		}

		private Builder(String entityTypeName, Object entityIdentifier,
				URLEncodedString elasticsearchIndexName,
				String documentIdentifier, String routingKey) {
			super( SUCCESS_ASSESSOR, entityTypeName, entityIdentifier, documentIdentifier );
			this.indexName = elasticsearchIndexName;
			this.routingKey = routingKey;
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			JsonObject delete = new JsonObject();
			delete.addProperty( "_index", indexName.original );
			delete.addProperty( "_id", documentIdentifier );

			if ( routingKey != null ) {
				delete.addProperty( "routing", routingKey );
			}

			JsonObject result = new JsonObject();
			result.add( "delete", delete );

			return result;
		}

		@Override
		protected JsonObject buildBulkableActionBody() {
			return null;
		}

		@Override
		public DeleteWork build() {
			return new DeleteWork( this );
		}
	}
}
