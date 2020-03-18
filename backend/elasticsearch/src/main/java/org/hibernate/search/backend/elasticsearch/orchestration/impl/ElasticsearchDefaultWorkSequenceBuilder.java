/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResultItemExtractor;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A simple implementation of {@link ElasticsearchWorkSequenceBuilder}.
 */
class ElasticsearchDefaultWorkSequenceBuilder implements ElasticsearchWorkSequenceBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Supplier<ElasticsearchWorkExecutionContext> contextSupplier;
	private final BulkResultExtractionStepImpl bulkResultExtractionStep = new BulkResultExtractionStepImpl();

	private CompletableFuture<?> currentlyBuildingSequenceTail;
	private SequenceContext currentlyBuildingSequenceContext;

	ElasticsearchDefaultWorkSequenceBuilder(Supplier<ElasticsearchWorkExecutionContext> contextSupplier) {
		this.contextSupplier = contextSupplier;
	}

	@Override
	public void init(CompletableFuture<?> previous) {
		// We only use the previous stage to delay the execution of the sequence, but we ignore its result
		this.currentlyBuildingSequenceTail = previous.handle( (ignoredResult, ignoredThrowable) -> null );
		this.currentlyBuildingSequenceContext = new SequenceContext(
				contextSupplier.get()
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

		// If the previous work failed, then skip the new work and notify the caller and failure handler as necessary.
		CompletableFuture<T> handledWorkExecutionFuture = currentlyBuildingSequenceTail
				.whenComplete( Futures.handler( workExecutionState::onPreviousWorkComplete ) )
				// If the previous work completed normally, then execute the new work
				.thenCompose( Futures.safeComposer( workExecutionState::onPreviousWorkSuccess ) );

		/*
		 * Make sure that the sequence will only advance to the next work
		 * after both the work and *all* the handlers are executed,
		 * because otherwise failureHandler.handle() could be called before all failed/skipped works are reported.
		 */
		currentlyBuildingSequenceTail = handledWorkExecutionFuture;

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
		final SequenceContext currentSequenceContext = this.currentlyBuildingSequenceContext;

		CompletableFuture<BulkResult> bulkWorkResultFuture =
				// When the previous work completes successfully *and* the bulk work is available...
				currentlyBuildingSequenceTail.thenCombine( workFuture, (ignored, work) -> work )
				// ... execute the bulk work
				.thenCompose( currentSequenceContext::execute );
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
		final SequenceContext currentSequenceContext = this.currentlyBuildingSequenceContext;

		CompletableFuture<BulkResultItemExtractor> extractorFuture =
				bulkResultFuture.thenApply( currentSequenceContext::addContext );
		bulkResultExtractionStep.init( extractorFuture );
		return bulkResultExtractionStep;
	}

	@Override
	public CompletableFuture<Void> build() {
		// Use a local variable to make sure lambdas (if any) won't be affected by a reset()
		final SequenceContext sequenceContext = currentlyBuildingSequenceContext;

		return currentlyBuildingSequenceTail.handle( Futures.handler(
				(BiFunction<Object, Throwable, Void>) sequenceContext::onSequenceFinished
		) );
	}

	private final class BulkResultExtractionStepImpl implements BulkResultExtractionStep {

		private CompletableFuture<BulkResultItemExtractor> extractorFuture;

		void init(CompletableFuture<BulkResultItemExtractor> extractorFuture) {
			this.extractorFuture = extractorFuture;
		}

		@Override
		public <T> CompletableFuture<T> add(BulkableWork<T> bulkedWork, int index) {
			// Use local variables to make sure the lambdas won't be affected by a reset()
			final SequenceContext sequenceContext = ElasticsearchDefaultWorkSequenceBuilder.this.currentlyBuildingSequenceContext;

			BulkedWorkExecutionState<T> workExecutionState =
					new BulkedWorkExecutionState<>( sequenceContext, bulkedWork, index );

			// If the bulk work fails, make sure to notify the caller and failure handler as necessary.
			CompletableFuture<T> handledWorkExecutionFuture = extractorFuture
					.whenComplete( Futures.handler( workExecutionState::onBulkWorkComplete ) )
					// If the bulk work succeeds, then extract the bulked work result and notify as necessary
					.thenCompose( workExecutionState::onBulkWorkSuccess );

			/*
			 * Make sure that the sequence will only advance to the next work
			 * after both the work and *all* the handlers are executed,
			 * because otherwise failureHandler.handle(...) could be called before all failed/skipped works are reported.
			 */
			currentlyBuildingSequenceTail = CompletableFuture.allOf(
					currentlyBuildingSequenceTail,
					handledWorkExecutionFuture
			);

			return workExecutionState.workFutureForCaller;
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
		private final ElasticsearchWorkExecutionContext executionContext;

		SequenceContext(ElasticsearchWorkExecutionContext executionContext) {
			this.executionContext = executionContext;
		}

		<T> CompletionStage<T> execute(NonBulkableWork<T> work) {
			return work.execute( executionContext );
		}

		public BulkResultItemExtractor addContext(BulkResult bulkResult) {
			return bulkResult.withContext( executionContext );
		}

		<T> T onSequenceFinished(Object ignored, Throwable throwable) {
			if ( throwable != null && !(throwable instanceof PreviousWorkException) ) {
				throw Throwables.toRuntimeException( throwable );
			}
			return null;
		}
	}

	private abstract static class AbstractWorkExecutionState<T, W extends ElasticsearchWork<T>> {

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

		protected void skip(Throwable throwable) {
			Throwable skippingCause = throwable instanceof PreviousWorkException ? throwable.getCause() : throwable;
			workFutureForCaller.completeExceptionally(
					log.elasticsearchSkippedBecauseOfPreviousWork( skippingCause )
			);
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

		void onPreviousWorkComplete(Object ignored, Throwable throwable) {
			if ( throwable != null ) {
				skip( throwable );
			}
		}

		CompletableFuture<R> onPreviousWorkSuccess(Object ignored) {
			CompletableFuture<R> workExecutionFuture = work.execute( sequenceContext.executionContext );
			return addPostExecutionHandlers( workExecutionFuture );
		}
	}

	private static final class BulkedWorkExecutionState<R> extends AbstractWorkExecutionState<R, BulkableWork<R>> {

		private final BulkableWork<R> bulkedWork;

		private final int index;

		private BulkResultItemExtractor extractor;

		private BulkedWorkExecutionState(SequenceContext sequenceContext,
				BulkableWork<R> bulkedWork, int index) {
			super( sequenceContext, bulkedWork );
			this.bulkedWork = bulkedWork;
			this.index = index;
		}

		void onBulkWorkComplete(BulkResultItemExtractor ignored, Throwable throwable) {
			if ( throwable == null ) {
				// No failure: nothing to handle.
				return;
			}
			else if ( throwable instanceof PreviousWorkException ) {
				// The bulk work itself was skipped; mark the bulked work as skipped too
				skip( throwable );
			}
			else {
				// The bulk work failed; mark the bulked work as failed too
				failBecauseBulkFailed( throwable );
			}
		}

		CompletableFuture<R> onBulkWorkSuccess(BulkResultItemExtractor extractor) {
			this.extractor = extractor;
			// Use Futures.create to catch any exception thrown by extractor.extract
			CompletableFuture<R> workExecutionFuture = Futures.create( this::extract );
			return addPostExecutionHandlers( workExecutionFuture );
		}

		private CompletableFuture<R> extract() {
			return CompletableFuture.completedFuture( extractor.extract( bulkedWork, index ) );
		}

		private void failBecauseBulkFailed(Throwable throwable) {
			fail( log.elasticsearchFailedBecauseOfBulkFailure( throwable ) );
		}
	}
}
