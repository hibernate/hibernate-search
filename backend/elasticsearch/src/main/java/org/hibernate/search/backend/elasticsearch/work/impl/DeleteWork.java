package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class DeleteWork extends AbstractSingleDocumentIndexingWork {

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			ElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	private DeleteWork(Builder builder) {
		super( builder );
	}

	public static class Builder extends AbstractSingleDocumentIndexingWork.AbstractBuilder<Builder> {

		private final URLEncodedString indexName;

		private final String routingKey;

		public static Builder create(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
				String documentIdentifier, String routingKey) {
			return new Builder( entityTypeName, entityIdentifier, elasticsearchIndexName, documentIdentifier, routingKey );
		}

		private Builder(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
				String documentIdentifier, String routingKey) {
			super( SUCCESS_ASSESSOR, entityTypeName, entityIdentifier, documentIdentifier );
			this.indexName = elasticsearchIndexName;
			this.routingKey = routingKey;
		}

		@Override
		protected ObjectNode buildBulkableActionMetadata() {
			ObjectNode delete = new ObjectNode( JsonNodeFactory.instance );
			delete.put( "_index", indexName.original );
			delete.put( "_id", documentIdentifier );
			if ( routingKey != null ) {
				delete.put( "routing", routingKey );
			}
			ObjectNode result = new ObjectNode( JsonNodeFactory.instance );
			result.set( "delete", delete );
			return result;
		}

		@Override
		protected ObjectNode buildBulkableActionBody() {
			return null;
		}

		@Override
		public DeleteWork build() {
			return new DeleteWork( this );
		}
	}
}
