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

/**
 * Aggregates works into a single sequence,
 * respecting the order they were submitted in.
 * <p>
 * Two works submitted to this orchestrator in the same batch will always be executed
 * one after the other, never in parallel.
 * <p>
 * This class is mutable and not thread-safe.
 */
class ElasticsearchSerialWorkProcessor implements ElasticsearchWorkProcessor {

	private final BulkAndSequenceAggregator aggregator;

	public ElasticsearchSerialWorkProcessor(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.aggregator = new BulkAndSequenceAggregator( sequenceBuilder, bulker );
	}

	@Override
	public void beginBatch() {
		aggregator.reset();
	}

	@Override
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
			if ( bulker.addWorksToSequence() ) {
				/*
				 * We want to execute works in the exact order they were received,
				 * so if a non-bulkable work is about to be added,
				 * we can't add any more works to the current bulk
				 * (otherwise the next bulked works may be executed
				 * before the current non-bulkable work).
				 */
				bulker.finalizeBulkWork();
			}
			return sequenceBuilder.addNonBulkExecution( work );
		}

		public CompletableFuture<Void> buildSequence() {
			bulker.addWorksToSequence();
			CompletableFuture<Void> future = sequenceBuilder.build();
			bulker.finalizeBulkWork();
			return future;
		}
	}
}