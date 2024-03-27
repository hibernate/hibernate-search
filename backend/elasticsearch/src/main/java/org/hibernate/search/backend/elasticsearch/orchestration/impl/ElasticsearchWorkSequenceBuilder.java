/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;

/**
 * Organizes the execution of works in a sequence,
 * and builds a {@link CompletableFuture} that will be completed when
 * all the works in the sequence will be completed.
 * <p>
 * Implementations do not aggregate works into bulks;
 * use {@link ElasticsearchDefaultWorkBulker} for that purpose.
 * <p>
 * When a work fails (throws an exception), the sequence itself fails
 * and the remaining works in the sequence are not executed,
 * though some may have already been executed if they were bulked together
 * with the failing work.
 * <p>
 * Implementations are mutable and unlikely to be thread-safe.
 *
 */
interface ElasticsearchWorkSequenceBuilder {

	/**
	 * Initialize the sequence.
	 * <p>
	 * Must be called before any attempt to add works to the sequence.
	 *
	 * @param previous the future that will need to complete successfully
	 * before any work in the sequence is executed.
	 */
	void init(CompletableFuture<?> previous);

	/**
	 * Add a step to execute a new work.
	 * <p>
	 * A failure in the previous work will lead to the new work being marked as skipped,
	 * and a failure during the new work will lead to the new work being marked
	 * as failed.
	 *
	 * @param work The work to be executed
	 * @return A future that will ultimately contain the result of executing the work, or an exception.
	 */
	<T> CompletableFuture<T> addNonBulkExecution(NonBulkableWork<T> work);

	/**
	 * Add a step to execute a bulk work.
	 * <p>
	 * The bulk work won't be marked as skipped or failed, regardless of errors.
	 * Only the bulked works will be marked (as skipped) if a previous work or the bulk work fails.
	 *
	 * @param workFuture The work to be executed
	 * @return A future for the result the bulk execution
	 */
	CompletableFuture<BulkResult> addBulkExecution(CompletableFuture<? extends NonBulkableWork<BulkResult>> workFuture);

	/**
	 * Add a bulked work whose result should be extracted.
	 * <p>
	 * <strong>WARNING:</strong> the bulk execution itself must be added to another sequence,
	 * or to the same sequence but <em>before</em> the result extraction,
	 * otherwise a deadlock will occur.
	 * <p>
	 * Extraction for this work will be triggered as soon as the bulk work completes,
	 * and will occur regardless of failures when extracting results for other bulked works.
	 * <p>
	 * A failure in the bulk work will lead to the bulked work being marked as skipped,
	 * and a failure during the result extraction of the bulked work
	 * will lead to the bulked work being marked as failed.
	 *
	 * @param bulkResultFuture The bulk work result, returned by a former call to {@link #addBulkExecution(CompletableFuture)}.
	 * @param bulkedWork The work whose result will be extracted.
	 * @param index The index of the bulked work in the bulk.
	 * @return A future that will ultimately contain the result of extracting the work result, or an exception.
	 */
	<T> CompletableFuture<T> addBulkResultExtraction(CompletableFuture<BulkResult> bulkResultFuture,
			BulkableWork<T> bulkedWork, int index);

	/**
	 * Build the resulting {@link CompletableFuture} for this sequence by adding some final steps if necessary.
	 * @return A completable future that will be complete when all the works in the sequence completed.
	 */
	CompletableFuture<Void> build();

}
