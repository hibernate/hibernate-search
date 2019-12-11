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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.IndexWorkBuilder;
import org.hibernate.search.engine.backend.common.DocumentReference;

import com.google.gson.JsonObject;


public class IndexWork extends AbstractSimpleBulkableElasticsearchWork<Void>
		implements SingleDocumentElasticsearchWork<Void> {

	private final String mappedTypeName;
	private final URLEncodedString id;

	private IndexWork(Builder builder) {
		super( builder );
		this.mappedTypeName = builder.mappedTypeName;
		this.id = builder.id;
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		return null;
	}

	@Override
	public DocumentReference getDocumentReference() {
		return new ElasticsearchDocumentReference( mappedTypeName, id.original );
	}

	public static class Builder
			extends AbstractSimpleBulkableElasticsearchWork.AbstractBuilder<Builder>
			implements IndexWorkBuilder {
		private final String mappedTypeName;
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final String routingKey;
		private final JsonObject document;

		public static Builder forElasticsearch67AndBelow(String mappedTypeName,
				URLEncodedString elasticsearchIndexName, URLEncodedString typeName, URLEncodedString id, String routingKey,
				JsonObject document) {
			return new Builder( mappedTypeName, elasticsearchIndexName, typeName, id, routingKey, document );
		}

		public static Builder forElasticsearch7AndAbove(String mappedTypeName,
				URLEncodedString elasticsearchIndexName, URLEncodedString id, String routingKey,
				JsonObject document) {
			return new Builder( mappedTypeName, elasticsearchIndexName, null, id, routingKey, document );
		}

		private Builder(String mappedTypeName, URLEncodedString elasticsearchIndexName,
					URLEncodedString typeName, URLEncodedString id, String routingKey, JsonObject document) {
			super( elasticsearchIndexName, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.mappedTypeName = mappedTypeName;
			this.indexName = elasticsearchIndexName;
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

			if ( routingKey != null ) {
				index.addProperty( "routing", routingKey );
			}

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