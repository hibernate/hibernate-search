/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.DeleteByQueryWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.RefreshWorkBuilder;

import com.google.gson.JsonObject;

/**
 * A delete by query work for ES5, using the core delete-by-query API.
 *
 */
public class DeleteByQueryWork extends AbstractSimpleElasticsearchWork<Void> {

	private final ElasticsearchWork<?> refreshWork;

	protected DeleteByQueryWork(Builder builder) {
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
			extends AbstractBuilder<Builder>
			implements DeleteByQueryWorkBuilder {
		private final URLEncodedString indexName;
		private final JsonObject payload;
		private final Set<URLEncodedString> typeNames = new HashSet<>();

		private final RefreshWorkBuilder refreshWorkBuilder;

		public Builder(URLEncodedString indexName, JsonObject payload, ElasticsearchWorkBuilderFactory workFactory) {
			super( indexName, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
			this.payload = payload;
			this.refreshWorkBuilder = workFactory.refresh().index( indexName );
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( indexName )
					/*
					 * Ignore conflicts: if we wrote to a document concurrently,
					 * we just want to keep it as is.
					 */
					.param( "conflicts", "proceed" );

			if ( !typeNames.isEmpty() ) {
				builder.multiValuedPathComponent( typeNames );
			}

			builder.pathComponent( Paths._DELETE_BY_QUERY )
					.body( payload );

			return builder.build();
		}

		protected ElasticsearchWork<?> buildRefreshWork() {
			return refreshWorkBuilder.build();
		}

		@Override
		public DeleteByQueryWork build() {
			return new DeleteByQueryWork( this );
		}
	}
}