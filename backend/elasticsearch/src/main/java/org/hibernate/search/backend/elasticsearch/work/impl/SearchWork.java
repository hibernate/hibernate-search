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

		public static <T> Builder<T> forElasticsearch6AndBelow(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			// No "track_total_hits": this parameter does not exist in ES6 and below, and total hits are always tracked
			return new Builder<>( payload, resultExtractor, null );
		}

		public static <T> Builder<T> forElasticsearch7AndAbove(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			// TODO HSEARCH-3517 disable track_total_hits when possible
			return new Builder<>( payload, resultExtractor, true );
		}

		private final JsonObject payload;
		private final ElasticsearchSearchResultExtractor<T> resultExtractor;
		private final Boolean trackTotalHits;
		private final Set<URLEncodedString> indexes = new HashSet<>();

		private Long from;
		private Long size;
		private Long scrollSize;
		private String scrollTimeout;
		private Set<String> routingKeys;

		private Builder(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor, Boolean trackTotalHits) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.payload = payload;
			this.resultExtractor = resultExtractor;
			this.trackTotalHits = trackTotalHits;
		}

		@Override
		public Builder<T> indexes(Collection<URLEncodedString> indexNames) {
			indexes.addAll( indexNames );
			return this;
		}

		@Override
		public Builder<T> paging(Long limit, Long offset) {
			this.from = offset;
			this.size = limit;
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

			if ( trackTotalHits != null ) {
				builder.param( "track_total_hits", trackTotalHits );
			}

			return builder.build();
		}

		@Override
		public SearchWork<T> build() {
			return new SearchWork<>( this );
		}
	}
}
