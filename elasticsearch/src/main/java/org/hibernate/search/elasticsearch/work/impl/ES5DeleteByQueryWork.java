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
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;

import com.google.gson.JsonObject;

/**
 * A delete by query work for ES5, using the core delete-by-query API.
 *
 * @author Yoann Rodiere
 */
public class ES5DeleteByQueryWork extends SimpleElasticsearchWork<Void> {

	private final ElasticsearchWork<?> refreshWork;

	protected ES5DeleteByQueryWork(Builder builder) {
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
			super( indexName, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
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
		public ES5DeleteByQueryWork build() {
			return new ES5DeleteByQueryWork( this );
		}
	}
}