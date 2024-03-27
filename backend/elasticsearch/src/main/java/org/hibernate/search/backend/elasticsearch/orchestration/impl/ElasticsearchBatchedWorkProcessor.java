/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWorkProcessor;

/**
 * A processor for batched works that triggers work execution
 * in the order they are submitted in.
 * <p>
 * Works are added by submitting as many works as necessary through {@link #submit(BulkableWork)}.
 * Execution starts as soon as possible,
 * which may be as late as when {@link #endBatch()} is called.
 * <p>
 * Two works submitted to this orchestrator in the same batch will always be executed
 * one after the other, never in parallel.
 * <p>
 * This class is mutable and not thread-safe.
 */
class ElasticsearchBatchedWorkProcessor implements BatchedWorkProcessor {

	private final ElasticsearchWorkSequenceBuilder sequenceBuilder;
	private final ElasticsearchWorkBulker bulker;

	public ElasticsearchBatchedWorkProcessor(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.sequenceBuilder = sequenceBuilder;
		this.bulker = bulker;
	}

	@Override
	public void beginBatch() {
		bulker.reset();
		sequenceBuilder.init( CompletableFuture.completedFuture( null ) );
	}

	public <T> CompletableFuture<T> submit(BulkableWork<T> work) {
		return bulker.add( work );
	}

	@Override
	public CompletableFuture<Void> endBatch() {
		CompletableFuture<Void> future = sequenceBuilder.build();
		bulker.finalizeBulkWork();
		// Sequence futures are not expected to fail even if one work fails,
		// so we can safely return this future directly.
		return future;
	}

	@Override
	public void complete() {
		// Nothing to do: if all individual works have completed, we're done.
	}
}
