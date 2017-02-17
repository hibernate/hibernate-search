/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteWorkBuilder;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class DeleteWork extends SimpleBulkableElasticsearchWork<Void> {

	private static final ElasticsearchRequestSuccessAssessor SUCCESS_ASSESSOR =
			DefaultElasticsearchRequestSuccessAssessor.builder().ignoreErrorStatuses( 404 ).build();

	public DeleteWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return null;
	}

	public static class Builder
			extends SimpleBulkableElasticsearchWork.Builder<Builder>
			implements DeleteWorkBuilder {
		private final String indexName;
		private final String typeName;
		private final String id;

		public Builder(String indexName, String typeName, String id) {
			super( indexName, SUCCESS_ASSESSOR );
			this.indexName = indexName;
			this.typeName = typeName;
			this.id = id;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.delete()
					.pathComponent( indexName )
					.pathComponent( typeName )
					.pathComponent( id );
			return builder.build();
		}

		@Override
		protected JsonObject buildBulkableActionMetadata() {
			return JsonBuilder.object()
					.add( "delete", JsonBuilder.object()
							.addProperty( "_index", indexName )
							.addProperty( "_type", typeName )
							.addProperty( "_id", id )
					)
					.build();
		}

		@Override
		public DeleteWork build() {
			return new DeleteWork( this );
		}
	}
}