/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;

import com.google.gson.JsonObject;

public class DeleteByQueryWork extends AbstractNonBulkableWork<Void> {

	private final NonBulkableWork<?> refreshWork;

	protected DeleteByQueryWork(Builder builder) {
		super( builder );
		this.refreshWork = builder.buildRefreshWork();
	}

	@Override
	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext,
			ElasticsearchRequest request) {
		if ( refreshWork != null ) {
			// Refresh the index in order to minimize the risk of version conflict.
			return refreshWork.execute( executionContext );
		}
		else {
			// Refresh is not supported, just hope for the best.
			return CompletableFuture.completedFuture( null );
		}
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder> {
		private final URLEncodedString indexName;
		private final JsonObject payload;
		private final Set<URLEncodedString> typeNames = new HashSet<>();

		private Collection<String> routingKeys;

		private final RefreshWork.Builder refreshWorkBuilder;

		public Builder(URLEncodedString indexName, JsonObject payload, ElasticsearchWorkFactory workFactory) {
			super( ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE );
			this.indexName = indexName;
			this.payload = payload;
			this.refreshWorkBuilder = workFactory.isRefreshSupported() ? workFactory.refresh().index( indexName ) : null;
		}

		public Builder routingKeys(Collection<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
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

			if ( routingKeys != null && !routingKeys.isEmpty() ) {
				builder.multiValuedParam( "routing", routingKeys );
			}

			builder.pathComponent( Paths._DELETE_BY_QUERY )
					.body( payload );

			return builder.build();
		}

		protected NonBulkableWork<?> buildRefreshWork() {
			return refreshWorkBuilder != null ? refreshWorkBuilder.build() : null;
		}

		@Override
		public DeleteByQueryWork build() {
			return new DeleteByQueryWork( this );
		}
	}
}
