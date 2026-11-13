package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

/**
 * @author Gunnar Morling
 */
public abstract class AbstractNonBulkableWork<R> implements NonBulkableWork<R> {

	private static final CompletableFuture<Void> SUCCESSFUL_FUTURE = CompletableFuture.completedFuture( null );

	protected final ElasticsearchJsonRequest request;
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

	public ElasticsearchJsonRequest request() {
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
					throw ElasticsearchClientLog.INSTANCE.elasticsearchRequestFailed(
							new org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchRequestFormatter( request )
									.toString(),
							new org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchResponseFormatter( null )
									.toString(),
							throwable.getMessage(),
							Throwables.expectException( throwable )
					);
				} ) )
				.thenApply( response -> handleResult( executionContext, response ) );
	}

	protected CompletableFuture<?> beforeExecute(ElasticsearchWorkExecutionContext executionContext,
			ElasticsearchJsonRequest request) {
		// Do nothing by default
		return SUCCESSFUL_FUTURE;
	}

	protected abstract R generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchJsonResponse response);

	private R handleResult(ElasticsearchWorkExecutionContext executionContext, ElasticsearchJsonResponse response) {
		R result;
		try {
			resultAssessor.checkSuccess( response );

			result = generateResult( executionContext, response );
		}
		catch (RuntimeException e) {
			throw ElasticsearchClientLog.INSTANCE.elasticsearchRequestFailed(
					new org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchRequestFormatter( request )
							.toString(),
					new org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchResponseFormatter( response )
							.toString(),
					e.getMessage(), e );
		}

		return result;
	}

	@SuppressWarnings("unchecked") // By contract, subclasses must implement B
	protected abstract static class AbstractBuilder<B> {
		protected ElasticsearchRequestSuccessAssessor resultAssessor;

		private Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> requestTransformer;

		public AbstractBuilder(ElasticsearchRequestSuccessAssessor resultAssessor) {
			this.resultAssessor = resultAssessor;
		}

		public B requestTransformer(Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> requestTransformer) {
			this.requestTransformer = requestTransformer;
			return (B) this;
		}

		private ElasticsearchJsonRequest buildRequestAndTransformIfNecessary() {
			ElasticsearchJsonRequest request = buildRequest();
			if ( requestTransformer != null ) {
				request = requestTransformer.apply( request );
			}
			return request;
		}

		protected abstract ElasticsearchJsonRequest buildRequest();

		public abstract AbstractNonBulkableWork<?> build();

	}
}
