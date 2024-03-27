/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A simple implementation of {@link ElasticsearchWorkSequenceBuilder}.
 */
class ElasticsearchDefaultWorkSequenceBuilder implements ElasticsearchWorkSequenceBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchWorkExecutionContext context;

	private SequenceContext currentlyBuildingSequenceContext;

	ElasticsearchDefaultWorkSequenceBuilder(ElasticsearchWorkExecutionContext context) {
		this.context = context;
	}

	@Override
	public void init(CompletableFuture<?> previous) {
		this.currentlyBuildingSequenceContext = new SequenceContext(
				context,
				// We only use the previous stage to delay the execution of the sequence, but we ignore its result
				previous.handle( (ignoredResult, ignoredThrowable) -> null )
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
	public <T> CompletableFuture<T> addNonBulkExecution(NonBulkableWork<T> work) {
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext sequenceContext = this.currentlyBuildingSequenceContext;

		NonBulkedWorkExecutionState<T> workExecutionState =
				new NonBulkedWorkExecutionState<>( sequenceContext, work );

		CompletableFuture<T> handledWorkExecutionFuture = sequenceContext.tail
				// When the previous work completes, execute the new work and notify as necessary.
				.thenCompose( Futures.safeComposer( workExecutionState::onPreviousWorkComplete ) );

		sequenceContext.updateTail( handledWorkExecutionFuture );

		return workExecutionState.workFutureForCaller;
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
	public CompletableFuture<BulkResult> addBulkExecution(CompletableFuture<? extends NonBulkableWork<BulkResult>> workFuture) {
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext sequenceContext = this.currentlyBuildingSequenceContext;

		CompletableFuture<BulkResult> bulkWorkResultFuture =
				// When the previous work completes *and* the bulk work is available...
				sequenceContext.tail.thenCombine( workFuture, (ignored, work) -> work )
						// ... execute the bulk work
						.thenCompose( sequenceContext::execute );

		sequenceContext.updateTail( bulkWorkResultFuture );

		return bulkWorkResultFuture;
	}

	@Override
	public <T> CompletableFuture<T> addBulkResultExtraction(CompletableFuture<BulkResult> bulkResultFuture,
			BulkableWork<T> bulkedWork, int index) {
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext sequenceContext = this.currentlyBuildingSequenceContext;

		CompletableFuture<BulkResult> delayedBulkResultFuture =
				// Only start extraction after the previous work is complete, so as to comply with the sequence order.
				sequenceContext.tail.thenCombine( bulkResultFuture, (ignored, bulkResult) -> bulkResult );

		BulkedWorkExecutionState<T> workExecutionState =
				new BulkedWorkExecutionState<>( sequenceContext, bulkedWork, index );

		CompletableFuture<T> handledWorkExecutionFuture = delayedBulkResultFuture
				// If the bulk work fails, make sure to notify the caller as necessary.
				.whenComplete( Futures.handler( workExecutionState::onBulkWorkComplete ) )
				// If the bulk work succeeds, then extract the bulked work result and notify as necessary.
				.thenCompose( workExecutionState::onBulkWorkSuccess );

		sequenceContext.updateTail(
				CompletableFuture.allOf( sequenceContext.tail, handledWorkExecutionFuture )
		);

		return workExecutionState.workFutureForCaller;
	}

	@Override
	public CompletableFuture<Void> build() {
		return currentlyBuildingSequenceContext.tail;
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
		private final ElasticsearchWorkExecutionContext executionContext;
		private CompletableFuture<Void> tail;

		SequenceContext(ElasticsearchWorkExecutionContext executionContext,
				CompletableFuture<?> previous) {
			this.executionContext = executionContext;
			updateTail( previous );
		}

		<T> CompletionStage<T> execute(NonBulkableWork<T> work) {
			return work.execute( executionContext );
		}

		void updateTail(CompletableFuture<?> workFuture) {
			// The result of the work is expected to be already reported when "workFuture" completes,
			// successfully or not.
			// Ignore any exception in following works in this sequence,
			// to make sure the following works will execute regardless of the failures in previous works
			// (but will still execute *after* previous works).
			tail = workFuture.handle( (ignoredResult, ignoredThrowable) -> null );
		}
	}

	private abstract static class AbstractWorkExecutionState<T, W extends ElasticsearchWork> {

		protected final SequenceContext sequenceContext;

		protected final W work;

		/*
		 * Use a different future for the caller than the one used in the sequence,
		 * because we manipulate internal exceptions in the sequence
		 * that should not be exposed to the caller.
		 */
		final CompletableFuture<T> workFutureForCaller = new CompletableFuture<>();

		private AbstractWorkExecutionState(SequenceContext sequenceContext, W work) {
			this.sequenceContext = sequenceContext;
			this.work = work;
		}

		protected CompletableFuture<T> addPostExecutionHandlers(CompletableFuture<T> workExecutionFuture) {
			/*
			 * In case of success, propagate the result to the client.
			 */
			workExecutionFuture.whenComplete( Futures.copyHandler( workFutureForCaller ) );
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
			return workExecutionFuture.exceptionally( Futures.handler( this::fail ) );
		}

		protected T fail(Throwable throwable) {
			workFutureForCaller.completeExceptionally( throwable );
			throw new PreviousWorkException( throwable );
		}
	}

	private static final class NonBulkedWorkExecutionState<R> extends AbstractWorkExecutionState<R, NonBulkableWork<R>> {

		private NonBulkedWorkExecutionState(SequenceContext sequenceContext, NonBulkableWork<R> work) {
			super( sequenceContext, work );
		}

		CompletableFuture<R> onPreviousWorkComplete(Object ignored) {
			CompletableFuture<R> workExecutionFuture = work.execute( sequenceContext.executionContext );
			return addPostExecutionHandlers( workExecutionFuture );
		}
	}

	private static final class BulkedWorkExecutionState<R> extends AbstractWorkExecutionState<R, BulkableWork<R>> {

		private final BulkableWork<R> bulkedWork;

		private final int index;

		private BulkResult bulkResult;

		private BulkedWorkExecutionState(SequenceContext sequenceContext,
				BulkableWork<R> bulkedWork, int index) {
			super( sequenceContext, bulkedWork );
			this.bulkedWork = bulkedWork;
			this.index = index;
		}

		void onBulkWorkComplete(Object ignored, Throwable throwable) {
			if ( throwable != null ) {
				// The bulk work failed; mark the bulked work as failed too
				fail( log.elasticsearchFailedBecauseOfBulkFailure( throwable.getMessage(), throwable ) );
			}
		}

		CompletableFuture<R> onBulkWorkSuccess(BulkResult bulkResult) {
			this.bulkResult = bulkResult;
			// Use Futures.create to catch any exception thrown by extractor.extract
			CompletableFuture<R> workExecutionFuture = Futures.create( this::extract );
			return addPostExecutionHandlers( workExecutionFuture );
		}

		private CompletableFuture<R> extract() {
			return CompletableFuture.completedFuture(
					bulkResult.extract( sequenceContext.executionContext, bulkedWork, index )
			);
		}
	}
}
