/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.BulkWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.Throwables;

import java.lang.invoke.MethodHandles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class BulkWork implements ElasticsearchWork<BulkResult> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<JsonArray> BULK_ITEMS = JsonAccessor.root().property( "items" ).asArray();

	private final ElasticsearchRequest request;

	private final List<BulkableElasticsearchWork<?>> works;

	/**
	 * Whether to perform a refresh in the course of executing this bulk or not.
	 * <p>
	 * Note that this will refresh all indexes touched by this bulk,
	 * not only those given via {@link #indexesNeedingRefresh}. That's acceptable.
	 * <p>
	 * If {@code true}, no additional refresh of the concerned indexes
	 * is needed after executing the bulk.
	 */
	private final boolean refreshInAPICall;

	protected BulkWork(Builder builder) {
		super();
		this.request = builder.buildRequest();
		this.works = new ArrayList<>( builder.bulkableWorks );
		this.refreshInAPICall = builder.refreshInBulkAPICall;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( "works = " ).append( works )
				.append( ", refreshInAPICall = " ).append( refreshInAPICall )
				.append( "]" )
				.toString();
	}

	@Override
	public CompletableFuture<BulkResult> execute(ElasticsearchWorkExecutionContext context) {
		return Futures.create( () -> context.getClient().submit( request ) )
				.thenApply( this::generateResult )
				.exceptionally( Futures.handler(
						throwable -> {
							throw log.elasticsearchRequestFailed( request, null, Throwables.expectException( throwable ) ); }
				) );
	}

	@Override
	public CompletableFuture<BulkResult> aggregate(ElasticsearchWorkAggregator aggregator) {
		return aggregator.addNonBulkable( this );
	}

	@Override
	public Object getInfo() {
		return null;
	}

	private BulkResult generateResult(ElasticsearchResponse response) {
		JsonObject parsedResponseBody = response.getBody();
		JsonArray resultItems = BULK_ITEMS.get( parsedResponseBody ).orElseGet( JsonArray::new );
		return new BulkResultDefualt( resultItems, refreshInAPICall );
	}

	private static class NoIndexDirtyBulkExecutionContext extends ElasticsearchForwardingWorkExecutionContext {

		public NoIndexDirtyBulkExecutionContext(ElasticsearchWorkExecutionContext delegate) {
			super( delegate );
		}

		@Override
		public void registerIndexToRefresh(URLEncodedString indexName) {
			// Don't delegate
		}
	}

	public static class Builder implements BulkWorkBuilder {
		private final List<? extends BulkableElasticsearchWork<?>> bulkableWorks;
		private boolean refreshInBulkAPICall;

		public Builder(List<? extends BulkableElasticsearchWork<?>> bulkableWorks) {
			this.bulkableWorks = bulkableWorks;
		}

		@Override
		public Builder refresh(boolean refresh) {
			this.refreshInBulkAPICall = refresh;
			return this;
		}

		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.post()
					.pathComponent( Paths._BULK )
					.param( "refresh", refreshInBulkAPICall );

			for ( BulkableElasticsearchWork<?> work : bulkableWorks ) {
				builder.body( work.getBulkableActionMetadata() );
				JsonObject actionBody = work.getBulkableActionBody();
				if ( actionBody != null ) {
					builder.body( actionBody );
				}
			}

			return builder.build();
		}

		@Override
		public BulkWork build() {
			return new BulkWork( this );
		}
	}

	private static class BulkResultDefualt implements BulkResult {
		private final JsonArray results;
		private final boolean refreshInAPICall;

		public BulkResultDefualt(JsonArray results, boolean refreshInAPICall) {
			super();
			this.results = results;
			this.refreshInAPICall = refreshInAPICall;
		}

		@Override
		public BulkResultItemExtractor withContext(ElasticsearchWorkExecutionContext context) {
			ElasticsearchWorkExecutionContext actualContext;
			if ( refreshInAPICall ) {
				/*
				 * Prevent bulked works to mark indexes as dirty,
				 * since we refresh all indexes as part of the Bulk API call.
				 */
				actualContext = new NoIndexDirtyBulkExecutionContext( context );
			}
			else {
				actualContext = context;
			}
			return new BulkItemResultExtractorDefault( results, actualContext );
		}
	}

	private static class BulkItemResultExtractorDefault implements BulkResultItemExtractor {
		private final JsonArray results;

		private final ElasticsearchWorkExecutionContext context;


		public BulkItemResultExtractorDefault(JsonArray results, ElasticsearchWorkExecutionContext context) {
			super();
			this.results = results;
			this.context = context;
		}

		@Override
		public <T> CompletableFuture<T> extract(BulkableElasticsearchWork<T> work, int index) {
			JsonObject bulkItemResponse = results.get( index ).getAsJsonObject();
			return work.handleBulkResult( context, bulkItemResponse );
		}

	}

}
