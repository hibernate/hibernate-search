/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.elasticsearch.work.impl.BulkResult;
import org.hibernate.search.elasticsearch.work.impl.BulkResultItemExtractor;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.Futures;

/**
 * A simple implementation of {@link ElasticsearchWorkSequenceBuilder}.
 * <p>
 * Execution of works will not be performed by this class;
 * instead, it delegates to an "executor" passed to the constructor.
 * <p>
 * Works will be executed inside a sequence-scoped context (a {@link FlushableElasticsearchWorkExecutionContext}),
 * ultimately leading to a {@link FlushableElasticsearchWorkExecutionContext#flush()}.
 */
class DefaultElasticsearchWorkSequenceBuilder implements ElasticsearchWorkSequenceBuilder {

	private final ElasticsearchWorkExecutor executor;
	private final Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplier;
	private final Supplier<ContextualErrorHandler> errorHandlerSupplier;
	private final BulkResultExtractionStepImpl bulkResultExtractionStep = new BulkResultExtractionStepImpl();

	private CompletableFuture<Void> rootSequenceFuture;
	private CompletableFuture<?> sequenceFuture;
	private FlushableElasticsearchWorkExecutionContext executionContext;
	private ContextualErrorHandler errorHandler;

	public DefaultElasticsearchWorkSequenceBuilder(ElasticsearchWorkExecutor executor,
			Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplier,
			Supplier<ContextualErrorHandler> errorHandlerSupplier) {
		this.executor = executor;
		this.contextSupplier = contextSupplier;
		this.errorHandlerSupplier = errorHandlerSupplier;
	}

	@Override
	public void init(CompletableFuture<?> previous) {
		this.rootSequenceFuture = previous.handle( (ignoredResult, ignoredThrowable) -> null );
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
	public <T> void addNonBulkExecution(ElasticsearchWork<T> work) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final FlushableElasticsearchWorkExecutionContext context = this.executionContext;
		this.sequenceFuture = chain( this.sequenceFuture, work, ignored -> executor.submit( work, context ) );
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
		final FlushableElasticsearchWorkExecutionContext context = this.executionContext;
		CompletableFuture<BulkResult> bulkWorkResultFuture =
				// When the previous work completes successfully *and* the bulk work is available...
				sequenceFuture.thenCombine( workFuture, (ignored, work) -> work )
				// ... execute the bulk work
				.thenCompose( work -> executor.submit( work, context ) );
		this.sequenceFuture = bulkWorkResultFuture;
		return bulkWorkResultFuture;
	}

	@Override
	public BulkResultExtractionStep startBulkResultExtraction(CompletableFuture<BulkResult> bulkResultFuture) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final FlushableElasticsearchWorkExecutionContext context = this.executionContext;
		CompletableFuture<BulkResultItemExtractor> extractorFuture =
				bulkResultFuture.thenApply( bulkResult -> bulkResult.withContext( context ) );
		bulkResultExtractionStep.init( extractorFuture );
		return bulkResultExtractionStep;
	}

	@Override
	public CompletableFuture<Void> build() {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final FlushableElasticsearchWorkExecutionContext context = this.executionContext;
		final ContextualErrorHandler errorHandler = this.errorHandler;
		CompletableFuture<Void> futureWithFlush = Futures.whenCompleteExecute(
						sequenceFuture,
						() -> context.flush()
				)
				.exceptionally( Futures.handler( throwable -> {
					if ( !( throwable instanceof PreviousWorkException) ) {
						// Something else than a work failed, mention it
						errorHandler.addThrowable( throwable );
					}
					errorHandler.handle();
					return null;
				} ) );

		return futureWithFlush;
	}

	private <T, R> CompletableFuture<R> chain(CompletableFuture<T> previous, ElasticsearchWork<R> work,
			Function<T, CompletableFuture<R>> composer) {
		// Use local variables to make sure the lambdas won't be affected by a reset()
		final ContextualErrorHandler errorHandler = this.errorHandler;

		final Function<T, CompletionStage<R>> safeComposer = Futures.safeComposer( composer );

		return previous
				// If the previous work failed, the new one won't execute, so mark it as such
				.whenComplete( (result, throwable) -> {
					if ( throwable != null ) {
						errorHandler.markAsSkipped( work );
					}
				} )
				// If the previous work completed normally, then execute the work
				.thenCompose( previousResult ->
						/*
						 * Add an error handler to the result of executing an Elasticsearch work.
						 *
						 * Note that the error handler is applied to the "inner" (to be composed) work,
						 * so that the handler is only executed if *this* work fails,
						 * not if a previous one fails.
						 *
						 * Note that the error handler won't stop the exception from propagating
						 * to the next works, so they will be skipped.
						 */
						safeComposer.apply( previousResult )
								.handle( Futures.handler( (result, throwable) -> {
									if ( throwable != null ) {
										errorHandler.markAsFailed( work, throwable );
										throw new PreviousWorkException( throwable );
									}
									else {
										return result;
									}
								} ) )
				);
	}

	private static final class PreviousWorkException extends SearchException {

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
		public <T> void add(BulkableElasticsearchWork<T> bulkedWork, int index) {
			CompletableFuture<T> bulkedWorkResultFuture = chain( extractorFuture, bulkedWork,
					extractor -> extractor.extract( bulkedWork, index ) );
			DefaultElasticsearchWorkSequenceBuilder.this.sequenceFuture =
					CompletableFuture.allOf( DefaultElasticsearchWorkSequenceBuilder.this.sequenceFuture, bulkedWorkResultFuture );
		}

	}

}
