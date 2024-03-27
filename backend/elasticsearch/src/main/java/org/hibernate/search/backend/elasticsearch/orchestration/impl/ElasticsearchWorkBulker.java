/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;

/**
 * Aggregates bulkable works into bulks and adds all resulting works
 * to a sequence builder.
 * <p>
 * Implementations are mutable and unlikely to be thread-safe.
 *
 */
public interface ElasticsearchWorkBulker {

	/**
	 * Add a bulkable work to the current bulk.
	 * <p>
	 * This method also takes care of adding the bulk work execution to the current sequence if not already done,
	 * and to add the extraction of the bulkable work result to the current sequence.
	 *
	 * @param work A work to add to the current bulk
	 * @return A future that will ultimately contain the result of executing the work, or an exception.
	 */
	<T> CompletableFuture<T> add(BulkableWork<T> work);

	/**
	 * Ensure that the bulk work (if any) is created.
	 * <p>
	 * After this method is called, any new work added through {@link #add(BulkableWork)}
	 * will be added to a new bulk.
	 */
	void finalizeBulkWork();

	/**
	 * Reset internal state.
	 */
	void reset();

}
