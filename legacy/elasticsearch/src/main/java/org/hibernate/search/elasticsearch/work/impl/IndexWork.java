/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.IndexWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class IndexWork extends SimpleBulkableElasticsearchWork<Void> {

	protected final IndexingMonitor indexingMonitor;

	public IndexWork(Builder builder) {
		super( builder );
		this.indexingMonitor = builder.monitor;
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
	protected CompletableFuture<?> afterSuccess(ElasticsearchWorkExecutionContext context) {
		if ( indexingMonitor != null ) {
			IndexingMonitor bufferedIndexingMonitor = context.getBufferedIndexingMonitor( indexingMonitor );
			bufferedIndexingMonitor.documentsAdded( 1 );
		}
		return super.afterSuccess( context );
	}

	public static class Builder
			extends SimpleBulkableElasticsearchWork.Builder<Builder>
			implements IndexWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final JsonObject document;

		protected IndexingMonitor monitor;

		public Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, JsonObject document) {
			super( indexName, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.typeName = typeName;
			this.id = id;
			this.document = document;
		}

		@Override
		public Builder monitor(IndexingMonitor monitor) {
			this.monitor = monitor;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.put()
					.pathComponent( indexName )
					.pathComponent( typeName )
					.pathComponent( id )
					.body( document );
			return builder.build();
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			return JsonBuilder.object()
					.add( "index", JsonBuilder.object()
							.addProperty( "_index", indexName )
							.addProperty( "_type", typeName )
							.addProperty( "_id", id )
					)
					.build();
		}

		@Override
		public IndexWork build() {
			return new IndexWork( this );
		}
	}
}