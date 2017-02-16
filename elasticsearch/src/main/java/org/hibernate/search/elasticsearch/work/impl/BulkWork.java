/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchRequestUtils;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.BulkResult.BulkResultItem;
import io.searchbox.params.Parameters;

/**
 * @author Yoann Rodiere
 */
public class BulkWork implements ElasticsearchWork<BulkResult> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final Bulk request;

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
		this.request = builder.buildAction();
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
	public BulkResult execute(ElasticsearchWorkExecutionContext context) {
		if ( refreshInAPICall ) {
			/*
			 * Prevent bulked works to mark indexes as dirty,
			 * since we refresh all indexes as part of the Bulk API call.
			 */
			context = new NoIndexDirtyBulkExecutionContext( context );
		}

		BulkResult response;
		try {
			response = context.getClient().executeRequest( request );
		}
		catch (IOException e) {
			GsonService gsonService = context.getGsonService();
			throw LOG.elasticsearchRequestFailed( ElasticsearchRequestUtils.formatRequest( gsonService, request ), null, e );
		}

		handleResults( context, request, response );

		return response;
	}

	@Override
	public void aggregate(ElasticsearchWorkAggregator aggregator) {
		aggregator.addNonBulkable( this );
	}

	@Override
	public Stream<LuceneWork> getLuceneWorks() {
		Stream<LuceneWork> result = Stream.empty();
		for ( BulkableElasticsearchWork<?> work : works ) {
			result = Stream.concat( result, work.getLuceneWorks() );
		}
		return result;
	}

	/*
	 * Give the chance for every work to handle the result,
	 * making sure that exceptions are handled properly
	 * so that one failing handler will not prevent others from being called.
	 *
	 * If at least one work or its result handler failed,
	 * an exception will be thrown after every result has been handled.
	 */
	private void handleResults(ElasticsearchWorkExecutionContext context, Bulk request, BulkResult result) {
		Map<BulkableElasticsearchWork<?>, BulkResultItem> successfulItems =
				CollectionHelper.newHashMap( works.size() );

		List<BulkableElasticsearchWork<?>> erroneousItems = new ArrayList<>();
		int i = 0;

		List<RuntimeException> resultHandlingExceptions = null;
		for ( BulkResultItem resultItem : result.getItems() ) {
			BulkableElasticsearchWork<?> work = works.get( i );

			boolean success;
			try {
				success = work.handleBulkResult( context, resultItem );
			}
			catch (RuntimeException e) {
				if ( resultHandlingExceptions == null ) {
					resultHandlingExceptions = new ArrayList<>();
				}
				resultHandlingExceptions.add( e );
				success = false;
			}

			if ( success ) {
				successfulItems.put( work, resultItem );
			}
			else {
				erroneousItems.add( work );
			}

			++i;
		}

		if ( !erroneousItems.isEmpty() ) {
			GsonService gsonService = context.getGsonService();
			BulkRequestFailedException exception = LOG.elasticsearchBulkRequestFailed(
					ElasticsearchRequestUtils.formatRequest( gsonService, request ),
					ElasticsearchRequestUtils.formatResponse( gsonService, result ),
					successfulItems,
					erroneousItems
			);
			if ( resultHandlingExceptions != null ) {
				for ( Exception resultHandlingException : resultHandlingExceptions ) {
					exception.addSuppressed( resultHandlingException );
				}
			}
			throw exception;
		}
	}

	private static class NoIndexDirtyBulkExecutionContext extends ForwardingElasticsearchWorkExecutionContext {

		public NoIndexDirtyBulkExecutionContext(ElasticsearchWorkExecutionContext delegate) {
			super( delegate );
		}

		@Override
		public void setIndexDirty(String indexName) {
			// Don't delegate
		}
	}

	public static class Builder {
		private final Bulk.Builder jestBuilder;
		private final List<BulkableElasticsearchWork<?>> bulkableWorks;
		private boolean refreshInBulkAPICall;

		public Builder(List<BulkableElasticsearchWork<?>> bulkableWorks) {
			this.bulkableWorks = bulkableWorks;
			this.jestBuilder = new Bulk.Builder();
		}

		public Builder refresh(boolean refresh) {
			this.refreshInBulkAPICall = refresh;
			return this;
		}

		protected Bulk buildAction() {
			for ( BulkableElasticsearchWork<?> work : bulkableWorks ) {
				jestBuilder.addAction( work.getBulkableAction() );
			}
			jestBuilder.setParameter( Parameters.REFRESH, refreshInBulkAPICall );
			return jestBuilder.build();
		}

		public BulkWork build() {
			return new BulkWork( this );
		}
	}
}
