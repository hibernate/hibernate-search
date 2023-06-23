/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Gunnar Morling
 */
public abstract class AbstractNonBulkableWork<R> implements NonBulkableWork<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final CompletableFuture<Void> SUCCESSFUL_FUTURE = CompletableFuture.completedFuture( null );

	protected final ElasticsearchRequest request;
	protected final ElasticsearchRequestSuccessAssessor resultAssessor;

	protected AbstractNonBulkableWork(AbstractBuilder<?> builder) {
		this.request = builder.buildRequestAndTransformIfNecessary();
		this.resultAssessor = builder.resultAssessor;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( "[" )
				.append( "path = " ).append( request.path() )
				.append( "]" )
				.toString();
	}

	public ElasticsearchRequest request() {
		return request;
	}

	@Override
	public final CompletableFuture<R> execute(ElasticsearchWorkExecutionContext executionContext) {
		return Futures.create( () -> beforeExecute( executionContext, request ) )
				.thenCompose( ignored -> executionContext.getClient().submit( request ) )
				.exceptionally( Futures.handler( throwable -> {
					// if we already have a SearchException, throw that,
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

	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext,
			ElasticsearchRequest request) {
		// Do nothing by default
		return SUCCESSFUL_FUTURE;
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response);

	private R handleResult(ElasticsearchWorkExecutionContext executionContext, ElasticsearchResponse response) {
		R result;
		try {
			resultAssessor.checkSuccess( response );

			result = generateResult( executionContext, response );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchRequestFailed( request, response, e.getMessage(), e );
		}

		return result;
	}

	@SuppressWarnings("unchecked") // By contract, subclasses must implement B
	protected abstract static class AbstractBuilder<B> {
		protected ElasticsearchRequestSuccessAssessor resultAssessor;

		private Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer;

		public AbstractBuilder(ElasticsearchRequestSuccessAssessor resultAssessor) {
			this.resultAssessor = resultAssessor;
		}

		public B requestTransformer(Function<ElasticsearchRequest, ElasticsearchRequest> requestTransformer) {
			this.requestTransformer = requestTransformer;
			return (B) this;
		}

		private ElasticsearchRequest buildRequestAndTransformIfNecessary() {
			ElasticsearchRequest request = buildRequest();
			if ( requestTransformer != null ) {
				request = requestTransformer.apply( request );
			}
			return request;
		}

		protected abstract ElasticsearchRequest buildRequest();

		public abstract AbstractNonBulkableWork<?> build();

	}
}
