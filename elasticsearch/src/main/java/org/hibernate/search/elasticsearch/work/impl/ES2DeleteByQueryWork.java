/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * A delete by query work for ES2, using the delete-by-query plugin.
 *
 * @author Yoann Rodiere
 */
public class ES2DeleteByQueryWork extends SimpleElasticsearchWork<Void> {

	protected ES2DeleteByQueryWork(Builder builder) {
		super( builder );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements DeleteByQueryWorkBuilder {
		private final String indexName;
		private final JsonObject payload;
		private List<String> typeNames = new ArrayList<>();

		public Builder(String indexName, JsonObject payload) {
			super( indexName, SuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.payload = payload;
		}

		@Override
		public Builder type(String typeName) {
			typeNames.add( typeName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.delete()
					.pathComponent( indexName );

			if ( !typeNames.isEmpty() ) {
				builder.multiValuedPathComponent( typeNames );
			}

			builder.pathComponent( "_query" )
					.body( payload );

			return builder.build();
		}

		@Override
		public ES2DeleteByQueryWork build() {
			return new ES2DeleteByQueryWork( this );
		}
	}

	private static class SuccessAssessor implements ElasticsearchRequestSuccessAssessor {

		private static final Log LOG = LoggerFactory.make( Log.class );

		private static final int NOT_FOUND_HTTP_STATUS_CODE = 404;

		public static final SuccessAssessor INSTANCE = new SuccessAssessor();

		private final DefaultElasticsearchRequestSuccessAssessor delegate;

		private SuccessAssessor() {
			this.delegate = DefaultElasticsearchRequestSuccessAssessor.builder( )
					.ignoreErrorStatuses( NOT_FOUND_HTTP_STATUS_CODE ).build();
		}

		@Override
		public void checkSuccess(ElasticsearchWorkExecutionContext context, ElasticsearchRequest request,
				Response response, JsonObject parsedResponseBody) throws SearchException {
			this.delegate.checkSuccess( context, request, response, parsedResponseBody );
			if ( response.getStatusLine().getStatusCode() == NOT_FOUND_HTTP_STATUS_CODE ) {
				GsonProvider gsonProvider = context.getGsonProvider();
				throw LOG.elasticsearch2RequestDeleteByQueryNotFound(
						ElasticsearchClientUtils.formatRequest( gsonProvider, request ),
						ElasticsearchClientUtils.formatResponse( gsonProvider, response, parsedResponseBody )
						);
			}
		}

		@Override
		public boolean isSuccess(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
			throw new AssertionFailure( "This method should never be called, because DeleteByQuery actions are not Bulkable" );
		}
	}
}