/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.Throwables;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public abstract class AbstractSimpleElasticsearchWork<R> implements ElasticsearchWork<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final CompletableFuture<Void> SUCCESSFUL_FUTURE = CompletableFuture.completedFuture( null );

	protected final ElasticsearchRequest request;
	protected final URLEncodedString dirtiedIndexName;
	protected final ElasticsearchRequestSuccessAssessor resultAssessor;
	protected final boolean markIndexDirty;

	protected AbstractSimpleElasticsearchWork(Builder<?> builder) {
		this.request = builder.buildRequest();
		this.dirtiedIndexName = builder.dirtiedIndexName;
		this.resultAssessor = builder.resultAssessor;
		this.markIndexDirty = builder.markIndexDirty;
	}

	@Override
	public Object getInfo() {
		// TODO extract immutable work relevant info. We need to think about it. See HSEARCH-3110.
		return this;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( "path = " ).append( request.getPath() )
				.append( ", dirtiedIndexName = " ).append( dirtiedIndexName )
				.append( "]" )
				.toString();
	}

	@Override
	public final CompletableFuture<R> execute(ElasticsearchWorkExecutionContext executionContext) {
		return Futures.create( () -> beforeExecute( executionContext, request ) )
				.thenCompose( ignored -> executionContext.getClient().submit( request ) )
				.exceptionally( Futures.handler(
						throwable -> { throw log.elasticsearchRequestFailed( request, null, Throwables.expectException( throwable ) ); }
				) )
				.thenCompose( response -> handleResult( executionContext, response ) );
	}

	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		// Do nothing by default
		return SUCCESSFUL_FUTURE;
	}

	protected CompletableFuture<?> afterSuccess(ElasticsearchWorkExecutionContext executionContext) {
		// Do nothing by default
		return SUCCESSFUL_FUTURE;
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response);

	@Override
	public CompletableFuture<R> aggregate(ElasticsearchWorkAggregator aggregator) {
		// May be overridden by subclasses
		return aggregator.addNonBulkable( this );
	}

	private CompletableFuture<R> handleResult(ElasticsearchWorkExecutionContext executionContext, ElasticsearchResponse response) {
		R result;
		try {
			resultAssessor.checkSuccess( response );

			result = generateResult( executionContext, response );

			if ( markIndexDirty ) {
				executionContext.registerIndexToRefresh( dirtiedIndexName );
			}
		}
		catch (RuntimeException e) {
			throw log.elasticsearchRequestFailed( request, response, e );
		}

		return afterSuccess( executionContext )
				.exceptionally( Futures.handler(
						throwable -> { throw log.elasticsearchRequestFailed( request, response, Throwables.expectException( throwable ) ); }
				) )
				.thenApply( ignored -> result );
	}

	@SuppressWarnings("unchecked") // By contract, subclasses must implement B
	protected abstract static class Builder<B> {
		protected final URLEncodedString dirtiedIndexName;
		protected ElasticsearchRequestSuccessAssessor resultAssessor;

		protected boolean markIndexDirty;

		public Builder(URLEncodedString dirtiedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor) {
			this.dirtiedIndexName = dirtiedIndexName;
			this.resultAssessor = resultAssessor;
		}

		public B markIndexDirty(boolean markIndexDirty) {
			this.markIndexDirty = markIndexDirty;
			return (B) this;
		}

		protected abstract ElasticsearchRequest buildRequest();

		public abstract ElasticsearchWork<?> build();
	}
}
