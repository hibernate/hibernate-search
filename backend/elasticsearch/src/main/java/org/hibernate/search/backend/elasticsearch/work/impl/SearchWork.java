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

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchLoadableSearchResult;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.util.common.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class SearchWork<T> extends AbstractSimpleElasticsearchWork<ElasticsearchLoadableSearchResult<T>> {

	private static final Log QUERY_LOG = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	private final ElasticsearchSearchResultExtractor<T> resultExtractor;

	protected SearchWork(Builder<T> builder) {
		super( builder );
		this.resultExtractor = builder.resultExtractor;
	}

	@Override
	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		QUERY_LOG.executingElasticsearchQuery(
				request.getPath(),
				request.getParameters(),
				executionContext.getGsonProvider().getLogHelper().toString( request.getBodyParts() )
				);
		return super.beforeExecute( executionContext, request );
	}

	@Override
	protected ElasticsearchLoadableSearchResult<T> generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		return resultExtractor.extract( body );
	}

	public static class Builder<T>
			extends AbstractBuilder<Builder<T>>
			implements SearchWorkBuilder<T> {
		private final JsonObject payload;
		private final ElasticsearchSearchResultExtractor<T> resultExtractor;
		private final Set<URLEncodedString> indexes = new HashSet<>();

		private Long from;
		private Long size;
		private Long scrollSize;
		private String scrollTimeout;
		private Set<String> routingKeys;

		public Builder(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.payload = payload;
			this.resultExtractor = resultExtractor;
		}

		@Override
		public Builder<T> indexes(Collection<URLEncodedString> indexNames) {
			indexes.addAll( indexNames );
			return this;
		}

		@Override
		public Builder<T> paging(Long firstResult, Long size) {
			this.from = firstResult;
			this.size = size;
			return this;
		}

		@Override
		public Builder<T> scrolling(long scrollSize, String scrollTimeout) {
			this.scrollSize = scrollSize;
			this.scrollTimeout = scrollTimeout;
			return this;
		}

		@Override
		public SearchWorkBuilder<T> routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.multiValuedPathComponent( indexes )
					.pathComponent( Paths._SEARCH )
					.body( payload );

			if ( from != null ) {
				builder.param( "from", from );
			}

			if ( size != null ) {
				builder.param( "size", size );
			}

			if ( scrollSize != null && scrollTimeout != null ) {
				builder.param( "size", scrollSize );
				builder.param( "scroll", scrollTimeout );
			}

			if ( !routingKeys.isEmpty() ) {
				builder.multiValuedParam( "routing", routingKeys );
			}

			return builder.build();
		}

		@Override
		public SearchWork<T> build() {
			return new SearchWork<>( this );
		}
	}
}
