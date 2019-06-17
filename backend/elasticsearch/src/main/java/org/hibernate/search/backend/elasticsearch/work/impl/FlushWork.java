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
import org.hibernate.search.backend.elasticsearch.work.builder.impl.FlushWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.RefreshWorkBuilder;

/**
 * A flush work for ES5, using the Flush API then the Refresh API.
 * <p>
 * This is necessary because the "refresh" parameter in the Flush API has been removed
 * in ES5 (elasticsearch/elasticsearch:7cc48c8e8723d3b31fbcb371070bc2a8d87b1f7e).
 *
 */
public class FlushWork extends AbstractSimpleElasticsearchWork<Void> {

	private final ElasticsearchWork<?> refreshWork;

	protected FlushWork(Builder builder) {
		super( builder );
		this.refreshWork = builder.buildRefreshWork();
	}

	@Override
	protected CompletableFuture<?> afterSuccess(ElasticsearchWorkExecutionContext executionContext) {
		return refreshWork.execute( executionContext );
	}

	@Override
	protected Void generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		return null;
	}

	public static class Builder
			extends AbstractBuilder<Builder>
			implements FlushWorkBuilder {
		private final RefreshWorkBuilder refreshWorkBuilder;
		private final Set<URLEncodedString> indexNames = new HashSet<>();

		public Builder(ElasticsearchWorkBuilderFactory workFactory) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.refreshWorkBuilder = workFactory.refresh();
		}

		@Override
		public Builder index(URLEncodedString indexName) {
			this.indexNames.add( indexName );
			this.refreshWorkBuilder.index( indexName );
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post();

			if ( !indexNames.isEmpty() ) {
				builder.multiValuedPathComponent( indexNames );
			}

			builder.pathComponent( Paths._FLUSH );

			return builder.build();
		}

		protected ElasticsearchWork<?> buildRefreshWork() {
			return refreshWorkBuilder.build();
		}

		@Override
		public FlushWork build() {
			return new FlushWork( this );
		}
	}
}