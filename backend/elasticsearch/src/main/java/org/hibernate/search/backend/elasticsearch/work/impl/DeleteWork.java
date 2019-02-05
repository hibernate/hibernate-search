/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class DeleteWork extends AbstractSimpleBulkableElasticsearchWork<Void> {

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			DefaultElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	public DeleteWork(Builder builder) {
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
			implements DeleteWorkBuilder {
		private final URLEncodedString indexName;
		private final URLEncodedString typeName;
		private final URLEncodedString id;
		private final String routingKey;

		public Builder(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String routingKey) {
			super( indexName, SUCCESS_ASSESSOR );
			this.indexName = indexName;
			this.typeName = typeName;
			this.id = id;
			this.routingKey = routingKey;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.delete()
					.pathComponent( indexName )
					.pathComponent( typeName )
					.pathComponent( id );

			if ( routingKey != null ) {
				builder.param( "routing", routingKey );
			}

			return builder.build();
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			JsonObject delete = new JsonObject();
			delete.addProperty( "_index", indexName.original );
			delete.addProperty( "_type", typeName.original );
			delete.addProperty( "_id", id.original );

			JsonObject result = new JsonObject();
			result.add( "delete", delete );

			return result;
		}

		@Override
		public DeleteWork build() {
			return new DeleteWork( this );
		}
	}
}