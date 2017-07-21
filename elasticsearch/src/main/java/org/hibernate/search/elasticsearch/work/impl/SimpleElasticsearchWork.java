/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.impl.Throwables;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public abstract class SimpleElasticsearchWork<R> implements ElasticsearchWork<R> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final CompletableFuture<Void> SUCCESSFUL_FUTURE = CompletableFuture.completedFuture( null );

	protected final ElasticsearchRequest request;
	private final LuceneWork luceneWork;
	protected final URLEncodedString dirtiedIndexName;
	protected final ElasticsearchRequestSuccessAssessor resultAssessor;
	protected final boolean markIndexDirty;

	protected SimpleElasticsearchWork(Builder<?> builder) {
		this.request = builder.buildRequest();
		this.luceneWork = builder.luceneWork;
		this.dirtiedIndexName = builder.dirtiedIndexName;
		this.resultAssessor = builder.resultAssessor;
		this.markIndexDirty = builder.markIndexDirty;
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
						throwable -> { throw LOG.elasticsearchRequestFailed( request, null, Throwables.expectException( throwable ) ); }
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
	public void aggregate(ElasticsearchWorkAggregator aggregator) {
		// May be overridden by subclasses
		aggregator.addNonBulkable( this );
	}

	@Override
	public LuceneWork getLuceneWork() {
		return luceneWork;
	}

	private CompletableFuture<R> handleResult(ElasticsearchWorkExecutionContext executionContext, ElasticsearchResponse response) {
		R result;
		try {
			resultAssessor.checkSuccess( response );

			result = generateResult( executionContext, response );

			if ( markIndexDirty ) {
				executionContext.setIndexDirty( dirtiedIndexName );
			}
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchRequestFailed( request, response, e );
		}

		return afterSuccess( executionContext )
				.exceptionally( Futures.handler(
						throwable -> { throw LOG.elasticsearchRequestFailed( request, response, Throwables.expectException( throwable ) ); }
				) )
				.thenApply( ignored -> result );
	}

	@SuppressWarnings("unchecked") // By contract, subclasses must implement B
	protected abstract static class Builder<B> {
		protected final URLEncodedString dirtiedIndexName;
		protected ElasticsearchRequestSuccessAssessor resultAssessor;

		protected LuceneWork luceneWork;
		protected boolean markIndexDirty;

		public Builder(URLEncodedString dirtiedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor) {
			this.dirtiedIndexName = dirtiedIndexName;
			this.resultAssessor = resultAssessor;
		}

		public B luceneWork(LuceneWork luceneWork) {
			this.luceneWork = luceneWork;
			return (B) this;
		}

		public B markIndexDirty(boolean markIndexDirty) {
			this.markIndexDirty = markIndexDirty;
			return (B) this;
		}

		protected abstract ElasticsearchRequest buildRequest();

		public abstract ElasticsearchWork<?> build();
	}
}
