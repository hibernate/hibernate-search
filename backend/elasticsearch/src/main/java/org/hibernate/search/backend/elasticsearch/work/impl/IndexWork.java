package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class IndexWork extends AbstractSingleDocumentIndexingWork implements SingleDocumentIndexingWork {

	private IndexWork(Builder builder) {
		super( builder );
	}

	public static class Builder extends AbstractSingleDocumentIndexingWork.AbstractBuilder<Builder> {

		private final URLEncodedString indexName;

		private final String routingKey;

		private final ObjectNode document;

		public static Builder create(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
				String documentIdentifier, String routingKey, ObjectNode document) {
			return new Builder( entityTypeName, entityIdentifier, elasticsearchIndexName, documentIdentifier, routingKey,
					document );
		}

		private Builder(String entityTypeName, Object entityIdentifier, URLEncodedString elasticsearchIndexName,
				String documentIdentifier, String routingKey, ObjectNode document) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE, entityTypeName, entityIdentifier, documentIdentifier );
			this.indexName = elasticsearchIndexName;
			this.routingKey = routingKey;
			this.document = document;
		}

		@Override
		protected ObjectNode buildBulkableActionMetadata() {
			ObjectNode index = new ObjectNode( JsonNodeFactory.instance );
			index.put( "_index", indexName.original );
			index.put( "_id", documentIdentifier );
			if ( routingKey != null ) {
				index.put( "routing", routingKey );
			}
			ObjectNode result = new ObjectNode( JsonNodeFactory.instance );
			result.set( "index", index );
			return result;
		}

		@Override
		protected ObjectNode buildBulkableActionBody() {
			return document;
		}

		@Override
		public IndexWork build() {
			return new IndexWork( this );
		}
	}
}
