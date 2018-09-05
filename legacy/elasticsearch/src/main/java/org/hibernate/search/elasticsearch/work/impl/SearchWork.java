/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.builder.SearchWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class SearchWork extends SimpleElasticsearchWork<SearchResult> {

	private static final Log QUERY_LOG = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	protected SearchWork(Builder builder) {
		super( builder );
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
	protected SearchResult generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
		JsonObject body = response.getBody();
		return new SearchResultImpl( body );
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements SearchWorkBuilder {
		private final JsonObject payload;
		private final Set<URLEncodedString> indexes = new HashSet<>();

		private Integer from;
		private Integer size;
		private Integer scrollSize;
		private String scrollTimeout;

		public Builder(JsonObject payload) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.payload = payload;
		}

		@Override
		public Builder indexes(Collection<URLEncodedString> indexNames) {
			indexes.addAll( indexNames );
			return this;
		}

		@Override
		public Builder paging(int firstResult, int size) {
			this.from = firstResult;
			this.size = size;
			return this;
		}

		@Override
		public Builder scrolling(int scrollSize, String scrollTimeout) {
			this.scrollSize = scrollSize;
			this.scrollTimeout = scrollTimeout;
			return this;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.multiValuedPathComponent( indexes )
					.pathComponent( Paths._SEARCH )
					.body( payload );

			if ( from != null && size != null ) {
				builder.param( "from", from );
				builder.param( "size", size );
			}

			if ( scrollSize != null && scrollTimeout != null ) {
				builder.param( "size", scrollSize );
				builder.param( "scroll", scrollTimeout );
			}

			return builder.build();
		}

		@Override
		public ElasticsearchWork<SearchResult> build() {
			return new SearchWork( this );
		}
	}

	static class SearchResultImpl implements SearchResult {

		private static final JsonAccessor<JsonArray> HITS_HITS_ACCESSOR = JsonAccessor.root().property( "hits" ).property( "hits" ).asArray();

		private static final JsonAccessor<Integer> COUNT_ACCESSOR = JsonAccessor.root().property( "hits" ).property( "total" ).asInteger();

		private static final JsonAccessor<JsonObject> AGGREGATIONS_ACCESSOR = JsonAccessor.root().property( "aggregations" ).asObject();

		private static final JsonAccessor<Integer> TOOK_ACCESSOR = JsonAccessor.root().property( "took" ).asInteger();

		private static final JsonAccessor<Boolean> TIMED_OUT_ACCESSOR = JsonAccessor.root().property( "timed_out" ).asBoolean();

		private static final JsonAccessor<String> SCROLL_ID_ACCESSOR = JsonAccessor.root().property( "_scroll_id" ).asString();

		private final JsonObject jsonObject;

		public SearchResultImpl(JsonObject jsonObject) {
			super();
			this.jsonObject = jsonObject;
		}

		@Override
		public JsonArray getHits() {
			return HITS_HITS_ACCESSOR.get( jsonObject )
					.orElseGet( JsonArray::new );
		}

		@Override
		public int getTotalHitCount() {
			return COUNT_ACCESSOR.get( jsonObject )
					.orElse( 0 );
		}

		@Override
		public JsonObject getAggregations() {
			return AGGREGATIONS_ACCESSOR.get( jsonObject )
					.orElseGet( JsonObject::new );
		}

		@Override
		public int getTook() {
			return TOOK_ACCESSOR.get( jsonObject )
					.orElseThrow( () -> new AssertionFailure(
							"Elasticsearch response lacked a value for '"
							+ TOOK_ACCESSOR.getStaticAbsolutePath() + "'"
					) );
		}

		@Override
		public boolean getTimedOut() {
			return TIMED_OUT_ACCESSOR.get( jsonObject )
					.orElseThrow( () -> new AssertionFailure(
							"Elasticsearch response lacked a value for '"
							+ TIMED_OUT_ACCESSOR.getStaticAbsolutePath() + "'"
					) );
		}

		@Override
		public String getScrollId() {
			return SCROLL_ID_ACCESSOR.get( jsonObject )
					.orElseThrow( () -> new AssertionFailure(
							"Elasticsearch response lacked a value for '"
							+ SCROLL_ID_ACCESSOR.getStaticAbsolutePath() + "'"
					) );
		}

	}
}
