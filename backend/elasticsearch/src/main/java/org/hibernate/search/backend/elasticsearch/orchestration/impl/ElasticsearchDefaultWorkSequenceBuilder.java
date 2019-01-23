/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A simple implementation of {@link ElasticsearchWorkSequenceBuilder}.
 * <p>
 * Works will be executed inside a sequence-scoped context (a {@link ElasticsearchRefreshableWorkExecutionContext}),
 * ultimately leading to a {@link ElasticsearchRefreshableWorkExecutionContext#executePendingRefreshes()}.
 */
class ElasticsearchDefaultWorkSequenceBuilder implements ElasticsearchWorkSequenceBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Supplier<ElasticsearchRefreshableWorkExecutionContext> contextSupplier;
	private final Supplier<ContextualErrorHandler> errorHandlerSupplier;
	private final BulkResultExtractionStepImpl bulkResultExtractionStep = new BulkResultExtractionStepImpl();

	private CompletableFuture<Void> refreshFuture;
	private CompletableFuture<?> sequenceFuture;
	private ElasticsearchRefreshableWorkExecutionContext executionContext;
	private ContextualErrorHandler errorHandler;

	public ElasticsearchDefaultWorkSequenceBuilder(Supplier<ElasticsearchRefreshableWorkExecutionContext> contextSupplier,
			Supplier<ContextualErrorHandler> errorHandlerSupplier) {
		this.contextSupplier = contextSupplier;
		this.errorHandlerSupplier = errorHandlerSupplier;
	}

	@Override
	public void init(CompletableFuture<?> previous) {
		CompletableFuture<Void> rootSequenceFuture = previous.handle( (ignoredResult, ignoredThrowable) -> null );
		this.refreshFuture = new CompletableFuture<>();
		this.sequenceFuture = rootSequenceFuture;
		this.executionContext = contextSupplier.get();
		this.errorHandler = errorHandlerSupplier.get();
	}

	/**
	 * Add a step to execute a new work.
	 * <p>
	 * A failure in the previous work will lead to the new work being marked as skipped,
	 * and a failure during the new work will lead to the new work being marked
	 * as failed.
	 *
	 * @param work The work to be executed
	 */
	@Override
	public <T> CompletableFuture<T> addNonBulkExecution(ElasticsearchWork<T> work) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final ElasticsearchRefreshableWorkExecutionContext context = this.executionContext;
		final ContextualErrorHandler errorHandler = this.errorHandler;

		/*
		 * Use a different future for the caller than the one used in the sequence,
		 * because we manipulate internal exceptions in the sequence
		 * that should not be exposed to the caller.
 		 */
		CompletableFuture<T> workFutureForCaller = new CompletableFuture<>();

		// If the previous work failed, then skip the new work and notify the caller and error handler as necessary.
		CompletableFuture<T> handledWorkExecutionFuture = this.sequenceFuture.whenComplete( Futures.handler( (ignoredResult, throwable) -> {
			if ( throwable != null ) {
				notifySkipping( work, throwable, errorHandler, workFutureForCaller );
			}
		} ) )
				// If the previous work completed normally, then execute the new work
				.thenCompose( Futures.safeComposer(
						ignoredPreviousResult -> {
							CompletableFuture<T> workExecutionFuture = work.execute( context );
							return addPostExecutionHandlers( work, workExecutionFuture, workFutureForCaller );
						}
				) );

		/*
		 * Make sure that the sequence will only advance to the next work
		 * after both the work and *all* the handlers are executed,
		 * because otherwise errorHandler.handle() could be called before all failed/skipped works are reported.
		 */
		this.sequenceFuture = handledWorkExecutionFuture;

		return workFutureForCaller;
	}

	/**
	 * Add a step to execute a bulk work.
	 * <p>
	 * The bulk work won't be marked as skipped or failed, regardless of errors.
	 * Only the bulked works will be marked (as skipped) if a previous work or the bulk work fails.
	 *
	 * @param workFuture The work to be executed
	 */
	@Override
	public CompletableFuture<BulkResult> addBulkExecution(CompletableFuture<? extends ElasticsearchWork<BulkResult>> workFuture) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final ElasticsearchRefreshableWorkExecutionContext context = this.executionContext;
		CompletableFuture<BulkResult> bulkWorkResultFuture =
				// When the previous work completes successfully *and* the bulk work is available...
				sequenceFuture.thenCombine( workFuture, (ignored, work) -> work )
				// ... execute the bulk work
				.thenCompose( work -> work.execute( context ) );
		this.sequenceFuture = bulkWorkResultFuture;
		return bulkWorkResultFuture;
	}

	@Override
	public BulkResultExtractionStep startBulkResultExtraction(CompletableFuture<BulkResult> bulkResultFuture) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final ElasticsearchWorkExecutionContext context = this.executionContext;
		CompletableFuture<BulkResultItemExtractor> extractorFuture =
				bulkResultFuture.thenApply( bulkResult -> bulkResult.withContext( context ) );
		bulkResultExtractionStep.init( extractorFuture );
		return bulkResultExtractionStep;
	}

	@Override
	public CompletableFuture<Void> build() {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final ElasticsearchRefreshableWorkExecutionContext context = this.executionContext;
		final ContextualErrorHandler errorHandler = this.errorHandler;
		final CompletableFuture<Void> refreshFuture = this.refreshFuture;

		CompletableFuture<Void> futureWithRefresh = Futures.whenCompleteExecute(
						sequenceFuture,
						() -> context.executePendingRefreshes().whenComplete( Futures.copyHandler( refreshFuture ) )
				)
				.exceptionally( Futures.handler( throwable -> {
					if ( !( throwable instanceof PreviousWorkException) ) {
						// Something else than a work failed, mention it
						errorHandler.addThrowable( throwable );
					}
					errorHandler.handle();
					return null;
				} ) );

		return futureWithRefresh;
	}

	private <R> void notifySkipping(ElasticsearchWork<R> work, Throwable throwable,
			ContextualErrorHandler errorHandler, CompletableFuture<R> workFutureForCaller) {
		Throwable skippingCause =
				throwable instanceof PreviousWorkException ? throwable.getCause() : throwable;
		workFutureForCaller.completeExceptionally(
				log.elasticsearchSkippedBecauseOfPreviousWork( skippingCause )
		);
		errorHandler.markAsSkipped( work );
	}

	private <R> void notifyFailure(ElasticsearchWork<R> work, Throwable throwable,
			ContextualErrorHandler errorHandler, CompletableFuture<R> workFutureForCaller) {
		workFutureForCaller.completeExceptionally( throwable );
		errorHandler.markAsFailed( work, throwable );
	}

	private <T> CompletableFuture<T> addPostExecutionHandlers(ElasticsearchWork<T> work,
			CompletableFuture<T> workExecutionFuture, CompletableFuture<T> workFutureForCaller) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final ContextualErrorHandler errorHandler = this.errorHandler;

		/*
		 * In case of success, wait for the refresh and propagate the result to the client.
		 * We ABSOLUTELY DO NOT WANT the resulting future to be included in the sequence,
		 * because it would create a deadlock:
		 * future A will only complete when the refresh future (B) is executed,
		 * which will only happen when the sequence ends,
		 * which will only happen after A completes...
		 */
		workExecutionFuture.thenCombine( refreshFuture, (workResult, refreshResult) -> workResult )
				.whenComplete( Futures.copyHandler( workFutureForCaller ) );
		/*
		 * In case of error, propagate the exception immediately to both the error handler and the client.
		 *
		 * Also, make sure to re-throw an exception
		 * so that execution of following works in the sequence will be skipped.
		 *
		 * Make sure to return the resulting stage, and not executedWorkStage,
		 * so that exception handling happens before the end of the sequence,
		 * meaning errorHandler.markAsFailed() is guaranteed to be called before errorHandler.handle().
		 */
		return workExecutionFuture.exceptionally( Futures.handler( throwable -> {
			notifyFailure( work, throwable, errorHandler, workFutureForCaller );
			throw new PreviousWorkException( throwable );
		} ) );
	}

	private static final class PreviousWorkException extends RuntimeException {

		public PreviousWorkException(Throwable cause) {
			super( cause );
		}

	}

	private final class BulkResultExtractionStepImpl implements BulkResultExtractionStep {

		private CompletableFuture<BulkResultItemExtractor> extractorFuture;

		public void init(CompletableFuture<BulkResultItemExtractor> extractorFuture) {
			this.extractorFuture = extractorFuture;
		}

		@Override
		public <T> CompletableFuture<T> add(BulkableElasticsearchWork<T> bulkedWork, int index) {
			// Use local variables to make sure the lambdas won't be affected by a reset()
			final ContextualErrorHandler errorHandler = ElasticsearchDefaultWorkSequenceBuilder.this.errorHandler;

			/*
			 * Use a different future for the caller than the one used in the sequence,
			 * because we manipulate internal exceptions in the sequence
			 * that should not be exposed to the caller.
			 */
			CompletableFuture<T> workFutureForCaller = new CompletableFuture<>();

			// If the bulk work fails, make sure to notify the caller and error handler as necessary.
			CompletableFuture<T> handledWorkExecutionFuture = extractorFuture.whenComplete( Futures.handler( (result, throwable) -> {
				if ( throwable == null ) {
					return;
				}
				else if ( throwable instanceof PreviousWorkException ) {
					// The bulk work itself was skipped; mark the bulked work as skipped too
					notifySkipping( bulkedWork, throwable, errorHandler, workFutureForCaller );
				}
				else {
					// The bulk work failed; mark the bulked work as failed too
					notifyFailure( bulkedWork, throwable, errorHandler, workFutureForCaller );
				}
			} ) )
					// If the bulk work succeeds, then extract the bulked work result and notify as necessary
					.thenCompose( extractor -> {
						// Use Futures.create to catch any exception thrown by extractor.extract
						CompletableFuture<T> workExecutionFuture = Futures.create(
								() -> extractor.extract( bulkedWork, index )
						);
						return addPostExecutionHandlers( bulkedWork, workExecutionFuture, workFutureForCaller );
					} );

			/*
			 * Make sure that the sequence will only advance to the next work
			 * after both the work and *all* the handlers are executed,
			 * because otherwise errorHandler.handle() could be called before all failed/skipped works are reported.
			 */
			ElasticsearchDefaultWorkSequenceBuilder.this.sequenceFuture = CompletableFuture.allOf(
					ElasticsearchDefaultWorkSequenceBuilder.this.sequenceFuture,
					handledWorkExecutionFuture
			);

			return workFutureForCaller;
		}

	}

}
