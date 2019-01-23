/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.Throwables;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public abstract class AbstractSimpleBulkableElasticsearchWork<R>
		extends AbstractSimpleElasticsearchWork<R>
		implements BulkableElasticsearchWork<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JsonObject bulkableActionMetadata;

	protected AbstractSimpleBulkableElasticsearchWork(Builder<?> builder) {
		super( builder );
		this.bulkableActionMetadata = builder.buildBulkableActionMetadata();
	}

	@Override
	public JsonObject getBulkableActionMetadata() {
		return bulkableActionMetadata;
	}

	@Override
	public JsonObject getBulkableActionBody() {
		List<JsonObject> bodyParts = request.getBodyParts();
		if ( !bodyParts.isEmpty() ) {
			if ( bodyParts.size() > 1 ) {
				throw new AssertionFailure( "Found a bulkable action with multiple body parts: " + bodyParts );
			}
			return bodyParts.get( 0 );
		}
		else {
			return null;
		}
	}

	@Override
	public CompletableFuture<R> handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem) {
		return Futures.create( () -> handleResult( context, bulkResponseItem ) );
	}

	@Override
	protected final CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		/*
		 * Making this method final so that it won't be overridden:
		 * this method is not used when the work is bulked
		 */
		return super.beforeExecute( executionContext, request );
	}

	@Override
	public CompletableFuture<R> aggregate(ElasticsearchWorkAggregator aggregator) {
		return aggregator.addBulkable( this );
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem);

	private CompletableFuture<R> handleResult(ElasticsearchWorkExecutionContext executionContext, JsonObject bulkResponseItem) {
		R result;
		try {
			resultAssessor.checkSuccess( bulkResponseItem );

			result = generateResult( executionContext, bulkResponseItem );

			if ( markIndexDirty ) {
				executionContext.registerIndexToRefresh( dirtiedIndexName );
			}
		}
		catch (RuntimeException e) {
			throw log.elasticsearchBulkedRequestFailed( getBulkableActionMetadata(), bulkResponseItem, e );
		}

		return afterSuccess( executionContext )
				.exceptionally( Futures.handler(
						throwable -> {
							throw log.elasticsearchBulkedRequestFailed(
									getBulkableActionMetadata(),
									bulkResponseItem, Throwables.expectException( throwable )
							);
						}
				) )
				.thenApply( ignored -> result );
	}

	protected abstract static class Builder<B>
			extends AbstractSimpleElasticsearchWork.Builder<B> {

		public Builder(URLEncodedString dirtiedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor) {
			super( dirtiedIndexName, resultAssessor );
		}

		protected abstract JsonObject buildBulkableActionMetadata();

	}
}
