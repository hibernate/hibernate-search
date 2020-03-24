/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkAggregator;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWorkProcessor;

/**
 * A processor for batched works that triggers work execution
 * in the order they are submitted in.
 * <p>
 * Works are added by submitting as many works as necessary through {@link #submit(ElasticsearchWork)}.
 * Execution starts as soon as possible,
 * which may be as late as when {@link #endBatch()} is called.
 * <p>
 * Two works submitted to this orchestrator in the same batch will always be executed
 * one after the other, never in parallel.
 * <p>
 * This class is mutable and not thread-safe.
 */
class ElasticsearchBatchedWorkProcessor implements BatchedWorkProcessor {

	private final BulkAndSequenceAggregator aggregator;

	public ElasticsearchBatchedWorkProcessor(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.aggregator = new BulkAndSequenceAggregator( sequenceBuilder, bulker );
	}

	@Override
	public void beginBatch() {
		aggregator.reset();
	}

	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		return work.aggregate( aggregator );
	}

	@Override
	public CompletableFuture<Void> endBatch() {
		// Sequence futures are not expected to fail even if one work fails,
		// so we can safely return this future directly.
		return aggregator.buildSequence();
	}

	@Override
	public void complete() {
		// Nothing to do: if all individual works have completed, we're done.
	}

	private static class BulkAndSequenceAggregator implements ElasticsearchWorkAggregator {

		private final ElasticsearchWorkSequenceBuilder sequenceBuilder;
		private final ElasticsearchWorkBulker bulker;

		public BulkAndSequenceAggregator(ElasticsearchWorkSequenceBuilder sequenceBuilder,
				ElasticsearchWorkBulker bulker) {
			super();
			this.sequenceBuilder = sequenceBuilder;
			this.bulker = bulker;
		}

		public void reset() {
			bulker.reset();
			sequenceBuilder.init( CompletableFuture.completedFuture( null ) );
		}

		@Override
		public <T> CompletableFuture<T> addBulkable(BulkableWork<T> work) {
			return bulker.add( work );
		}

		@Override
		public <T> CompletableFuture<T> addNonBulkable(NonBulkableWork<T> work) {
			/*
			 * We want to execute works in the exact order they were received,
			 * so if a non-bulkable work is about to be added,
			 * we can't add any more works to the current bulk
			 * (otherwise the next bulked works may be executed
			 * before the current non-bulkable work).
			 */
			bulker.finalizeBulkWork();
			return sequenceBuilder.addNonBulkExecution( work );
		}

		public CompletableFuture<Void> buildSequence() {
			CompletableFuture<Void> future = sequenceBuilder.build();
			bulker.finalizeBulkWork();
			return future;
		}
	}
}