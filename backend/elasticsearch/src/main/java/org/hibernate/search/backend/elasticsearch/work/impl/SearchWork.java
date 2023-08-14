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

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.util.common.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class SearchWork<R> extends AbstractNonBulkableWork<R> {

	private static final Log queryLog = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	private final ElasticsearchSearchResultExtractor<R> resultExtractor;
	private final Deadline deadline;
	private final boolean failOnDeadline;

	protected SearchWork(Builder<R> builder) {
		super( builder );
		this.resultExtractor = builder.resultExtractor;
		this.deadline = builder.deadline;
		this.failOnDeadline = builder.failOnDeadline;
	}

	@Override
	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext,
			ElasticsearchRequest request) {
		queryLog.executingElasticsearchQuery(
				request.path(),
				request.parameters(),
				executionContext.getGsonProvider().getLogHelper().toString( request.bodyParts() )
		);
		return super.beforeExecute( executionContext, request );
	}

	@Override
	protected R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.body();
		return resultExtractor.extract( body, failOnDeadline ? deadline : null );
	}

	public static class Builder<R>
			extends AbstractBuilder<Builder<R>> {

		public static <T> Builder<T> create(JsonObject payload, ElasticsearchSearchResultExtractor<T> resultExtractor) {
			return new Builder<>( payload, resultExtractor, true, false );
		}

		private final JsonObject payload;
		private final ElasticsearchSearchResultExtractor<R> resultExtractor;
		private final boolean allowPartialSearchResultsSupported;
		private final Set<URLEncodedString> indexes = new HashSet<>();

		private Boolean trackTotalHits;
		private Long totalHitCountThreshold;
		private Integer from;
		private Integer size;
		private Integer scrollSize;
		private String scrollTimeout;
		private Set<String> routingKeys;
		private Deadline deadline;
		private boolean failOnDeadline;

		private Builder(JsonObject payload, ElasticsearchSearchResultExtractor<R> resultExtractor, Boolean trackTotalHits,
				boolean allowPartialSearchResultsSupported) {
			super( ElasticsearchRequestSuccessAssessor.SHARD_FAILURE_CHECKED_INSTANCE );
			this.payload = payload;
			this.resultExtractor = resultExtractor;
			this.trackTotalHits = trackTotalHits;
			this.allowPartialSearchResultsSupported = allowPartialSearchResultsSupported;
		}

		public Builder<R> index(URLEncodedString indexName) {
			indexes.add( indexName );
			return this;
		}

		public Builder<R> paging(Integer limit, Integer offset) {
			this.from = offset;
			this.size = limit;
			return this;
		}

		public Builder<R> scrolling(int scrollSize, String scrollTimeout) {
			this.scrollSize = scrollSize;
			this.scrollTimeout = scrollTimeout;
			return this;
		}

		public Builder<R> routingKeys(Set<String> routingKeys) {
			this.routingKeys = routingKeys;
			return this;
		}

		public Builder<R> deadline(Deadline deadline, boolean failOnDeadline) {
			this.deadline = deadline;
			this.failOnDeadline = failOnDeadline;
			return this;
		}

		public Builder<R> disableTrackTotalHits() {
			// setting trackTotalHits to false only if this parameter was already set,
			// the parameter is not supported by the older Elasticsearch server
			if ( trackTotalHits != null && trackTotalHits ) {
				trackTotalHits = false;
			}
			return this;
		}

		public Builder<R> ignoreShardFailures() {
			resultAssessor = ElasticsearchRequestSuccessAssessor.DEFAULT_INSTANCE;
			return this;
		}

		public Builder<R> totalHitCountThreshold(Long totalHitCountThreshold) {
			// setting trackTotalHits to false only if this parameter was already set,
			// the parameter is not supported by the older Elasticsearch server
			if ( trackTotalHits != null && trackTotalHits ) {
				this.totalHitCountThreshold = totalHitCountThreshold;
			}
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
				if ( trackTotalHits && totalHitCountThreshold != null ) {
					// total hits is tracked but a with a limited precision
					builder.param( "track_total_hits", totalHitCountThreshold );
				}
				else {
					builder.param( "track_total_hits", trackTotalHits );
				}
			}

			handleDeadline( builder );

			return builder.build();
		}

		@Override
		public SearchWork<R> build() {
			return new SearchWork<>( this );
		}

		private void handleDeadline(ElasticsearchRequest.Builder builder) {
			if ( deadline == null ) {
				return;
			}

			// Client-side timeout: the search will fail on timeout.
			// This is necessary to address network problems: the server-side timeout would not detect that.
			if ( failOnDeadline ) {
				builder.deadline( deadline );
			}

			// Server-side timeout
			builder.param( "timeout", deadline.checkRemainingTimeMillis() + "ms" );
			if ( allowPartialSearchResultsSupported ) {
				// If failOnDeadline is true: ask the server to fail on timeout.
				// Functionally, this does not matter, because we also have a client-side timeout.
				// The server-side timeout is just an optimization so that Elasticsearch doesn't continue
				// to work on a search we cancelled on the client side.
				//
				// Otherwise: ask the server to truncate results on timeout.
				// This is normally the default behavior, but can be overridden with server-side settings,
				// so we set it just to be safe.
				builder.param( "allow_partial_search_results", !failOnDeadline );
			}
		}
	}
}
