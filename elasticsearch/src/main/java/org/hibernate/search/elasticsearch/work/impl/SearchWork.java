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

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.elasticsearch.work.impl.builder.SearchWorkBuilder;
import org.hibernate.search.util.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
	protected void beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		if ( QUERY_LOG.isDebugEnabled() ) {
			GsonProvider gsonProvider = executionContext.getGsonProvider();
			QUERY_LOG.executingElasticsearchQuery(
					request.getPath(),
					request.getParameters(),
					ElasticsearchClientUtils.formatRequestData( gsonProvider, request )
					);
		}
	}

	@Override
	protected SearchResult generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
		return new SearchResultImpl( parsedResponseBody );
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

		private static final JsonAccessor HITS_HITS_ACCESSOR = JsonAccessor.root().property( "hits" ).property( "hits" );

		private static final JsonAccessor COUNT_ACCESSOR = JsonAccessor.root().property( "hits" ).property( "total" );

		private static final JsonAccessor AGGREGATIONS_ACCESSOR = JsonAccessor.root().property( "aggregations" );

		private static final JsonAccessor TOOK_ACCESSOR = JsonAccessor.root().property( "took" );

		private static final JsonAccessor TIMED_OUT_ACCESSOR = JsonAccessor.root().property( "timed_out" );

		private static final JsonAccessor SCROLL_ID_ACCESSOR = JsonAccessor.root().property( "_scroll_id" );

		private final JsonObject jsonObject;

		public SearchResultImpl(JsonObject jsonObject) {
			super();
			this.jsonObject = jsonObject;
		}

		@Override
		public JsonArray getHits() {
			return HITS_HITS_ACCESSOR.get( jsonObject ).getAsJsonArray();
		}

		@Override
		public int getTotalHitCount() {
			return COUNT_ACCESSOR.get( jsonObject ).getAsInt();
		}

		@Override
		public JsonObject getAggregations() {
			JsonElement element = AGGREGATIONS_ACCESSOR.get( jsonObject );
			return element == null ? null : element.getAsJsonObject();
		}

		@Override
		public int getTook() {
			return TOOK_ACCESSOR.get( jsonObject ).getAsInt();
		}

		@Override
		public boolean getTimedOut() {
			return TIMED_OUT_ACCESSOR.get( jsonObject ).getAsBoolean();
		}

		@Override
		public String getScrollId() {
			JsonElement element = SCROLL_ID_ACCESSOR.get( jsonObject );
			return element == null ? null : element.getAsString();
		}

	}
}
