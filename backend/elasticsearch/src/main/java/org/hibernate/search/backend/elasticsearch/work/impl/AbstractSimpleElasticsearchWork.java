/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Gunnar Morling
 */
public abstract class AbstractSimpleElasticsearchWork<R> implements ElasticsearchWork<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final CompletableFuture<Void> SUCCESSFUL_FUTURE = CompletableFuture.completedFuture( null );

	protected final ElasticsearchRequest request;
	protected final URLEncodedString refreshedIndexName;
	protected final ElasticsearchRequestSuccessAssessor resultAssessor;
	protected final DocumentRefreshStrategy refreshStrategy;

	protected AbstractSimpleElasticsearchWork(AbstractBuilder<?> builder) {
		this.request = builder.buildRequestAndTransformIfNecessary();
		this.refreshedIndexName = builder.refreshedIndexName;
		this.resultAssessor = builder.resultAssessor;
		this.refreshStrategy = builder.refreshStrategy;
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
				.append( ", refreshedIndexName = " ).append( refreshedIndexName )
				.append( ", refreshStrategy = " ).append( refreshStrategy )
				.append( "]" )
				.toString();
	}

	@Override
	public final CompletableFuture<R> execute(ElasticsearchWorkExecutionContext executionContext) {
		return Futures.create( () -> beforeExecute( executionContext, request ) )
				.thenCompose( ignored -> executionContext.getClient().submit( request ) )
				.exceptionally( Futures.handler( throwable -> {
					// if we already have a SearchExececption, throw that,
					// since it will be more specific
					if ( throwable instanceof SearchException ) {
						throw (SearchException) throwable;
					}

					// otherwise, throw a more generic request failed exception
					throw log.elasticsearchRequestFailed(
							request, null,
							throwable.getMessage(),
							Throwables.expectException( throwable )
					);
				} ) )
				.thenApply( response -> handleResult( executionContext, response ) );
	}

	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext, ElasticsearchRequest request) {
		// Do nothing by default
		return SUCCESSFUL_FUTURE;
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response);

	@Override
	public CompletableFuture<R> aggregate(ElasticsearchWorkAggregator aggregator) {
		// May be overridden by subclasses
		return aggregator.addNonBulkable( this );
	}

	private R handleResult(ElasticsearchWorkExecutionContext executionContext, ElasticsearchResponse response) {
		R result;
		try {
			resultAssessor.checkSuccess( response );

			result = generateResult( executionContext, response );

			switch ( refreshStrategy ) {
				case FORCE:
					executionContext.registerIndexToRefresh( refreshedIndexName );
					break;
				case NONE:
					break;
			}
		}
		catch (RuntimeException e) {
			throw log.elasticsearchRequestFailed( request, response, e.getMessage(), e );
		}

		return result;
	}

	@SuppressWarnings("unchecked") // By contract, subclasses must implement B
	protected abstract static class AbstractBuilder<B> {
		protected final URLEncodedString refreshedIndexName;
		protected ElasticsearchRequestSuccessAssessor resultAssessor;

		protected DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		private Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer;

		public AbstractBuilder(URLEncodedString refreshedIndexName, ElasticsearchRequestSuccessAssessor resultAssessor) {
			this.refreshedIndexName = refreshedIndexName;
			this.resultAssessor = resultAssessor;
		}

		public B refresh(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return (B) this;
		}

		public B requestTransformer(Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer) {
			this.requestTransformer = requestTransformer;
			return (B) this;
		}

		protected static String getTimeoutString(Long timeoutValue, TimeUnit timeoutUnit) {
			StringBuilder builder = new StringBuilder( timeoutValue.toString() );
			switch ( timeoutUnit ) {
				case DAYS:
					builder.append( "d" );
					break;
				case HOURS:
					builder.append( "h" );
					break;
				case MINUTES:
					builder.append( "m" );
					break;
				case SECONDS:
					builder.append( "s" );
					break;
				case MILLISECONDS:
					builder.append( "ms" );
					break;
				case MICROSECONDS:
					builder.append( "micros" );
					break;
				case NANOSECONDS:
					builder.append( "nanos" );
					break;
			}
			return builder.toString();
		}

		private ElasticsearchRequest buildRequestAndTransformIfNecessary() {
			ElasticsearchRequest request = buildRequest();
			if ( requestTransformer != null ) {
				request = requestTransformer.apply( request );
			}
			return request;
		}

		protected abstract ElasticsearchRequest buildRequest();

		public abstract ElasticsearchWork<?> build();

	}
}
