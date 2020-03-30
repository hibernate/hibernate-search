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
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.SearchWorkBuilder;
import org.hibernate.search.util.common.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;


public class SearchWork<R> extends AbstractNonBulkableWork<R> {

	private static final Log queryLog = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	private final ElasticsearchSearchResultExtractor<R> resultExtractor;

	protected SearchWork(Builder<R> builder) {
		super( builder );
		this.resultExtractor = builder.resultExtractor;
	}

	@Override
	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		queryLog.executingElasticsearchQuery(
				request.getPath(),
				request.getParameters(),
				executionContext.getGsonProvider().getLogHelper().toString( request.getBodyParts() )
				);
		return super.beforeExecute( executionContext, request );
	}

	@Override
	protected R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		return resultExtractor.extract( body );
	}

	public static class Builder<R>
			extends AbstractBuilder<Builder<R>>
			implements SearchWorkBuilder<R> {

		public static <T> Builder<T> forElasticsearch62AndBelow(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			// No "track_total_hits": this parameter does not exist in ES6 and below, and total hits are always tracked
			// No "allow_partial_search_results": this parameter does not exist in ES6 and below, and total hits are always tracked
			// See https://github.com/elastic/elasticsearch/pull/27906
			return new Builder<>( payload, resultExtractor, null, false );
		}

		public static <T> Builder<T> forElasticsearch63to68(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			// No "track_total_hits": this parameter does not exist in ES6 and below, and total hits are always tracked
			return new Builder<>( payload, resultExtractor, null, false );
		}

		public static <T> Builder<T> forElasticsearch7AndAbove(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			// TODO HSEARCH-3517 disable track_total_hits when possible
			return new Builder<>( payload, resultExtractor, true, false );
		}

		private final JsonObject payload;
		private final ElasticsearchSearchResultExtractor<R> resultExtractor;
		private final Boolean trackTotalHits;
		private final boolean allowPartialSearchResultsSupported;
		private final Set<URLEncodedString> indexes = new HashSet<>();

		private Integer from;
		private Integer size;
		private Integer scrollSize;
		private String scrollTimeout;
		private Set<String> routingKeys;
		private Long timeoutValue;
		private TimeUnit timeoutUnit;
		private boolean exceptionOnTimeout;

		private Builder(JsonObject payload, ElasticsearchSearchResultExtractor<R> resultExtractor, Boolean trackTotalHits,
				boolean allowPartialSearchResultsSupported) {
			super( DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.payload = payload;
			this.resultExtractor = resultExtractor;
			this.trackTotalHits = trackTotalHits;
			this.allowPartialSearchResultsSupported = allowPartialSearchResultsSupported;
		}

		@Override
		public Builder<R> indexes(Collection<URLEncodedString> indexNames) {
			indexes.addAll( indexNames );
			return this;
		}

		@Override
		public Builder<R> paging(Integer limit, Integer offset) {
			this.from = offset;
			this.size = limit;
			return this;
		}

		@Override
		public Builder<R> scrolling(int scrollSize, String scrollTimeout) {
			this.scrollSize = scrollSize;
			this.scrollTimeout = scrollTimeout;
			return this;
		}

		@Override
		public SearchWorkBuilder<R> routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		@Override
		public SearchWorkBuilder<R> timeout(Long timeoutValue, TimeUnit timeoutUnit, boolean exceptionOnTimeout) {
			this.timeoutValue = timeoutValue;
			this.timeoutUnit = timeoutUnit;
			this.exceptionOnTimeout = exceptionOnTimeout;
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

			if ( routingKeys != null && !routingKeys.isEmpty() ) {
				builder.multiValuedParam( "routing", routingKeys );
			}

			if ( trackTotalHits != null ) {
				builder.param( "track_total_hits", trackTotalHits );
			}

			if ( timeoutValue != null && timeoutUnit != null ) {
				// Server-side timeout: the search will truncate results or fail on timeout.
				builder.param( "timeout", getTimeoutString( timeoutValue, timeoutUnit ) );
				if ( allowPartialSearchResultsSupported ) {
					if ( exceptionOnTimeout ) {
						// Ask the server to fail on timeout.
						// Functionally, this does not matter, because we also have a client-side timeout.
						// The server-side timeout is just an optimization so that Elasticsearch doesn't continue
						// to work on a search we cancelled on the client side.
						builder.param( "allow_partial_search_results", false );
					}
					else {
						// Ask the server to truncate results on timeout.
						// This is normally the default behavior, but can be overridden with server-side settings,
						// so we set it just to be safe.
						builder.param( "allow_partial_search_results", true );
					}
				}

				// Client-side timeout: the search will fail on timeout.
				// This is necessary to address network problems: the server-side timeout would not detect that.
				if ( exceptionOnTimeout ) {
					builder.timeout( timeoutValue, timeoutUnit );
				}
			}

			return builder.build();
		}

		@Override
		public SearchWork<R> build() {
			return new SearchWork<>( this );
		}
	}
}
