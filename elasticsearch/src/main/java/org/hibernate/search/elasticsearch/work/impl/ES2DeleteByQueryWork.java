/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
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

	private final ElasticsearchWork<?> refreshWork;

	protected ES2DeleteByQueryWork(Builder builder) {
		super( builder );
		this.refreshWork = builder.buildRefreshWork();
	}

	@Override
	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		/*
		 * Refresh the index so as to minimize the risk of version conflict
		 */
		return refreshWork.execute( executionContext );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements DeleteByQueryWorkBuilder {
		private final URLEncodedString indexName;
		private final JsonObject payload;
		private final Set<URLEncodedString> typeNames = new HashSet<>();

		private final RefreshWorkBuilder refreshWorkBuilder;

		public Builder(URLEncodedString indexName, JsonObject payload, ElasticsearchWorkFactory workFactory) {
			super( indexName, SuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.payload = payload;
			this.refreshWorkBuilder = workFactory.refresh().index( indexName );
		}

		@Override
		public Builder type(URLEncodedString typeName) {
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

			builder.pathComponent( Paths._QUERY )
					.body( payload );

			return builder.build();
		}

		protected ElasticsearchWork<?> buildRefreshWork() {
			return refreshWorkBuilder.build();
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
		public void checkSuccess(ElasticsearchResponse response) throws SearchException {
			this.delegate.checkSuccess( response );
			if ( response.getStatusCode() == NOT_FOUND_HTTP_STATUS_CODE ) {
				throw LOG.elasticsearch2RequestDeleteByQueryNotFound();
			}
		}

		@Override
		public void checkSuccess(JsonObject bulkResponseItem) {
			throw new AssertionFailure( "This method should never be called, because DeleteByQuery actions are not Bulkable" );
		}
	}
}