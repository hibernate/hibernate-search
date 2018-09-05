/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.work.impl.BulkResult;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;

/**
 * Organizes the execution of works in a sequence,
 * and builds a {@link CompletableFuture} that will be completed when
 * all the works in the sequence will be completed.
 * <p>
 * Implementations do not aggregate works into bulks;
 * use {@link DefaultElasticsearchWorkBulker} for that purpose.
 * <p>
 * When a work fails (throws an exception), the sequence itself fails
 * and the remaining works in the sequence are not executed,
 * though some may have already been executed if they were bulked together
 * with the failing work.
 * <p>
 * Implementations are mutable and unlikely to be thread-safe.
 *
 * @author Yoann Rodiere
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
	 */
	<T> void addNonBulkExecution(ElasticsearchWork<T> work);

	/**
	 * Add a step to execute a bulk work.
	 * <p>
	 * The bulk work won't be marked as skipped or failed, regardless of errors.
	 * Only the bulked works will be marked (as skipped) if a previous work or the bulk work fails.
	 *
	 * @param workFuture The work to be executed
	 * @return A future for the result the bulk execution
	 */
	CompletableFuture<BulkResult> addBulkExecution(CompletableFuture<? extends ElasticsearchWork<BulkResult>> workFuture);

	/**
	 * Add a step to extract the result of bulked works from the result of their bulk work.
	 * <p>
	 * <strong>WARNING:</strong> the bulk execution itself must be added to another sequence,
	 * or to the same sequence but <em>before</em> the result extraction,
	 * otherwise a deadlock will occur.
	 *
	 * @param bulkResultFuture The bulk work result
	 * @param bulkedWorks The bulked works whose results will be extracted
	 * @param offset the offset of the given works in the bulk result items
	 */
	BulkResultExtractionStep startBulkResultExtraction(CompletableFuture<BulkResult> bulkResultFuture);

	CompletableFuture<Void> build();

	interface BulkResultExtractionStep {

		/**
		 * Add a bulked work whose result should be extracted.
		 * <p>
		 * Extraction for this work will be triggered as soon as the bulk work completes,
		 * and will occur regardless of failures when extracting results for other bulked works.
		 * <p>
		 * A failure in the bulk work will lead to the bulked work being marked as skipped,
		 * and a failure during the result extraction of the bulked work
		 * will lead to the bulked work being marked as failed.
		 *
		 * @param bulkedWork The work whose result will be extracted.
		 * @param index The index of the bulked work in the bulk.
		 */
		<T> void add(BulkableElasticsearchWork<T> bulkedWork, int index);

	}

}
