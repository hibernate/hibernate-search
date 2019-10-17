/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.FailureContextImpl;
import org.hibernate.search.engine.reporting.spi.IndexFailureContextImpl;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A simple implementation of {@link ElasticsearchWorkSequenceBuilder}.
 * <p>
 * Works will be executed inside a sequence-scoped context (a {@link ElasticsearchRefreshableWorkExecutionContext}),
 * ultimately leading to a {@link ElasticsearchRefreshableWorkExecutionContext#executePendingRefreshes()}.
 */
class ElasticsearchDefaultWorkSequenceBuilder implements ElasticsearchWorkSequenceBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Supplier<ElasticsearchRefreshableWorkExecutionContext> contextSupplier;
	private final FailureHandler failureHandler;
	private final BulkResultExtractionStepImpl bulkResultExtractionStep = new BulkResultExtractionStepImpl();

	private CompletableFuture<?> currentlyBuildingSequenceTail;
	private SequenceContext currentlyBuildingSequenceContext;

	public ElasticsearchDefaultWorkSequenceBuilder(Supplier<ElasticsearchRefreshableWorkExecutionContext> contextSupplier,
			FailureHandler failureHandler) {
		this.contextSupplier = contextSupplier;
		this.failureHandler = failureHandler;
	}

	@Override
	public void init(CompletableFuture<?> previous) {
		// We only use the previous stage to delay the execution of the sequence, but we ignore its result
		this.currentlyBuildingSequenceTail = previous.handle( (ignoredResult, ignoredThrowable) -> null );
		this.currentlyBuildingSequenceContext = new SequenceContext(
				contextSupplier.get(), failureHandler
		);
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
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext sequenceContext = this.currentlyBuildingSequenceContext;

		/*
		 * Use a different future for the caller than the one used in the sequence,
		 * because we manipulate internal exceptions in the sequence
		 * that should not be exposed to the caller.
 		 */
		CompletableFuture<T> workFutureForCaller = new CompletableFuture<>();

		// If the previous work failed, then skip the new work and notify the caller and failure handler as necessary.
		CompletableFuture<T> handledWorkExecutionFuture = currentlyBuildingSequenceTail
				.whenComplete( Futures.handler( (ignoredResult, throwable) -> {
					if ( throwable != null ) {
						sequenceContext.notifyWorkSkipped( work, throwable, workFutureForCaller );
					}
				} ) )
				// If the previous work completed normally, then execute the new work
				.thenCompose( Futures.safeComposer(
						ignoredPreviousResult -> {
							CompletableFuture<T> workExecutionFuture = work.execute( sequenceContext.executionContext );
							return addPostExecutionHandlers( work, workExecutionFuture, workFutureForCaller, sequenceContext );
						}
				) );

		/*
		 * Make sure that the sequence will only advance to the next work
		 * after both the work and *all* the handlers are executed,
		 * because otherwise failureHandler.handle() could be called before all failed/skipped works are reported.
		 */
		currentlyBuildingSequenceTail = handledWorkExecutionFuture;

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
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext currentSequenceAttributes = this.currentlyBuildingSequenceContext;

		CompletableFuture<BulkResult> bulkWorkResultFuture =
				// When the previous work completes successfully *and* the bulk work is available...
				currentlyBuildingSequenceTail.thenCombine( workFuture, (ignored, work) -> work )
				// ... execute the bulk work
				.thenCompose( work -> work.execute( currentSequenceAttributes.executionContext ) );
		// Do not propagate the exception as is: we expect the exception to be handled by each bulked work separately.
		// ... but still propagate *something*, in case a *previous* work failed.
		currentlyBuildingSequenceTail = bulkWorkResultFuture.exceptionally( Futures.handler( throwable -> {
			throw new PreviousWorkException( throwable );
		} ) );
		return bulkWorkResultFuture;
	}

	@Override
	public BulkResultExtractionStep addBulkResultExtraction(CompletableFuture<BulkResult> bulkResultFuture) {
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext currentSequenceAttributes = this.currentlyBuildingSequenceContext;

		CompletableFuture<BulkResultItemExtractor> extractorFuture =
				bulkResultFuture.thenApply( bulkResult -> bulkResult.withContext( currentSequenceAttributes.executionContext ) );
		bulkResultExtractionStep.init( extractorFuture );
		return bulkResultExtractionStep;
	}

	@Override
	public CompletableFuture<Void> build() {
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext sequenceContext = currentlyBuildingSequenceContext;

		return Futures.whenCompleteExecute(
				currentlyBuildingSequenceTail,
				() -> sequenceContext.executionContext.executePendingRefreshes()
						.whenComplete( Futures.copyHandler( sequenceContext.refreshFuture ) )
		)
				.exceptionally( Futures.handler( t -> {
					sequenceContext.notifySequenceFailed( t );
					return null;
				} ) );
	}

	<T> CompletableFuture<T> addPostExecutionHandlers(ElasticsearchWork<T> work,
			CompletableFuture<T> workExecutionFuture, CompletableFuture<T> workFutureForCaller,
			SequenceContext sequenceContext) {
		/*
		 * In case of success, wait for the refresh and propagate the result to the client.
		 * We ABSOLUTELY DO NOT WANT the resulting future to be included in the sequence,
		 * because it would create a deadlock:
		 * future A will only complete when the refresh future (B) is executed,
		 * which will only happen when the sequence ends,
		 * which will only happen after A completes...
		 */
		workExecutionFuture.thenCombine( sequenceContext.refreshFuture, (workResult, refreshResult) -> workResult )
				.whenComplete( Futures.copyHandler( workFutureForCaller ) );
		/*
		 * In case of error, propagate the exception immediately to both the failure handler and the client.
		 *
		 * Also, make sure to re-throw an exception
		 * so that execution of following works in the sequence will be skipped.
		 *
		 * Make sure to return the resulting stage, and not executedWorkStage,
		 * so that exception handling happens before the end of the sequence,
		 * meaning notifyWorkFailed() is guaranteed to be called before notifySequenceFailed().
		 */
		return workExecutionFuture.exceptionally( Futures.handler( throwable -> {
			sequenceContext.notifyWorkFailed( work, throwable, workFutureForCaller );
			throw new PreviousWorkException( throwable );
		} ) );
	}

	private final class BulkResultExtractionStepImpl implements BulkResultExtractionStep {

		private CompletableFuture<BulkResultItemExtractor> extractorFuture;

		void init(CompletableFuture<BulkResultItemExtractor> extractorFuture) {
			this.extractorFuture = extractorFuture;
		}

		@Override
		public <T> CompletableFuture<T> add(BulkableElasticsearchWork<T> bulkedWork, int index) {
			// Use local variables to make sure the lambdas won't be affected by a reset()
			final SequenceContext sequenceContext = ElasticsearchDefaultWorkSequenceBuilder.this.currentlyBuildingSequenceContext;

			/*
			 * Use a different future for the caller than the one used in the sequence,
			 * because we manipulate internal exceptions in the sequence
			 * that should not be exposed to the caller.
			 */
			CompletableFuture<T> workFutureForCaller = new CompletableFuture<>();

			// If the bulk work fails, make sure to notify the caller and failure handler as necessary.
			CompletableFuture<T> handledWorkExecutionFuture = extractorFuture
					.whenComplete( Futures.handler( (result, throwable) -> {
						if ( throwable == null ) {
							return;
						}
						else if ( throwable instanceof PreviousWorkException ) {
							// The bulk work itself was skipped; mark the bulked work as skipped too
							sequenceContext.notifyWorkSkipped( bulkedWork, throwable, workFutureForCaller );
						}
						else {
							// The bulk work failed; mark the bulked work as failed too
							sequenceContext.notifyWorkFailed( bulkedWork, throwable, workFutureForCaller );
						}
					} ) )
					// If the bulk work succeeds, then extract the bulked work result and notify as necessary
					.thenCompose( extractor -> {
						// Use Futures.create to catch any exception thrown by extractor.extract
						CompletableFuture<T> workExecutionFuture = Futures.create(
								() -> extractor.extract( bulkedWork, index )
						);
						return addPostExecutionHandlers( bulkedWork, workExecutionFuture, workFutureForCaller, sequenceContext );
					} );

			/*
			 * Make sure that the sequence will only advance to the next work
			 * after both the work and *all* the handlers are executed,
			 * because otherwise failureHandler.handle(...) could be called before all failed/skipped works are reported.
			 */
			currentlyBuildingSequenceTail = CompletableFuture.allOf(
					currentlyBuildingSequenceTail,
					handledWorkExecutionFuture
			);

			return workFutureForCaller;
		}

	}

	private static final class PreviousWorkException extends RuntimeException {

		public PreviousWorkException(Throwable cause) {
			super( cause );
		}

	}

	/**
	 * Regroups all objects that may be shared among multiple steps in the same sequence.
	 * <p>
	 * This was introduced to make references to data from a previous sequence less likely;
	 * see
	 * org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchDefaultWorkSequenceBuilderTest#intertwinedSequenceExecution()
	 * for an example of what can go wrong if we don't take care to avoid that.
	 */
	private static final class SequenceContext {
		private final ElasticsearchRefreshableWorkExecutionContext executionContext;
		private final FailureHandler failureHandler;
		private final CompletableFuture<Void> refreshFuture;
		/*
		 * There may be multiple failures in a sequence, but only when works are bulked.
		 *
		 * This list and builders may be used from different threads handling different works,
		 * but never concurrently,
		 * because each work is executed strictly after the previous one.
		 */
		private volatile List<IndexFailureContextImpl.Builder> failureContextBuilders;

		SequenceContext(ElasticsearchRefreshableWorkExecutionContext executionContext,
				FailureHandler failureHandler) {
			this.executionContext = executionContext;
			this.failureHandler = failureHandler;
			this.refreshFuture = new CompletableFuture<>();
		}

		<R> void notifyWorkSkipped(ElasticsearchWork<R> work, Throwable throwable,
				CompletableFuture<R> workFutureForCaller) {
			Throwable skippingCause = throwable instanceof PreviousWorkException ? throwable.getCause() : throwable;
			workFutureForCaller.completeExceptionally(
					log.elasticsearchSkippedBecauseOfPreviousWork( skippingCause )
			);
			// Consider that skipped works were skipped because of the very first failure in the sequence.
			IndexFailureContextImpl.Builder contextBuilder = getFirstFailureContextBuilder();
			contextBuilder.uncommittedOperation( work.getInfo() );
		}

		<R> void notifyWorkFailed(ElasticsearchWork<R> work, Throwable throwable,
				CompletableFuture<R> workFutureForCaller) {
			workFutureForCaller.completeExceptionally( throwable );
			IndexFailureContextImpl.Builder contextBuilder = addFailure();
			contextBuilder.throwable( throwable );
			Object workInfo = work.getInfo();
			contextBuilder.failingOperation( workInfo );
			contextBuilder.uncommittedOperation( workInfo );
		}

		void notifySequenceFailed(Throwable throwable) {
			if ( failureContextBuilders != null ) {
				for ( IndexFailureContextImpl.Builder failureContextBuilder : failureContextBuilders ) {
					failureHandler.handle( failureContextBuilder.build() );
				}
			}
			if ( !( throwable instanceof PreviousWorkException) ) {
				// Something else than a work failed, mention it
				FailureContextImpl.Builder failureContextBuilder = new FailureContextImpl.Builder();
				failureContextBuilder.throwable( throwable );
				failureHandler.handle( failureContextBuilder.build() );
			}
		}

		private IndexFailureContextImpl.Builder addFailure() {
			if ( failureContextBuilders == null ) {
				failureContextBuilders = new ArrayList<>();
			}
			IndexFailureContextImpl.Builder builder = new IndexFailureContextImpl.Builder();
			failureContextBuilders.add( builder );
			return builder;
		}

		private IndexFailureContextImpl.Builder getFirstFailureContextBuilder() {
			if ( failureContextBuilders != null && !failureContextBuilders.isEmpty() ) {
				return failureContextBuilders.get( 0 );
			}
			else {
				// Shouldn't happen, but let's not throw exceptions while reporting failures...
				return addFailure();
			}
		}
	}
}
