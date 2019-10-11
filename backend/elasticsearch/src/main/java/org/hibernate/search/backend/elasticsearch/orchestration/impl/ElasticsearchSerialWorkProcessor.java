/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkAggregator;

/**
 * Aggregates works from worksets into a single sequence,
 * respecting the order they were submitted.
 * <p>
 * Two worksets or works submitted to this orchestrator will never run in parallel,
 * even if a reset() occurred between the two submissions.
 * <p>
 * This class is mutable and not thread-safe.
 *
 */
class ElasticsearchSerialWorkProcessor implements ElasticsearchWorkProcessor {

	private final BulkAndSequenceAggregator aggregator;

	private CompletableFuture<Void> future = CompletableFuture.completedFuture( null );

	public ElasticsearchSerialWorkProcessor(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.aggregator = new BulkAndSequenceAggregator( sequenceBuilder, bulker );
	}

	@Override
	public CompletableFuture<Void> submit(List<ElasticsearchWork<?>> nonBulkedWorks) {
		aggregator.initSequence( future );
		for ( ElasticsearchWork<?> work : nonBulkedWorks ) {
			work.aggregate( aggregator );
		}
		CompletableFuture<Void> sequenceFuture = aggregator.buildSequence();
		appendSequence( sequenceFuture );
		return sequenceFuture;
	}

	@Override
	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		aggregator.initSequence( future );
		CompletableFuture<T> workFuture = work.aggregate( aggregator );
		CompletableFuture<Void> sequenceFuture = aggregator.buildSequence();
		appendSequence( sequenceFuture );
		return workFuture;
	}

	@Override
	public CompletableFuture<Void> endBatch() {
		aggregator.finalizeBulkWork();
		return future;
	}

	@Override
	public void beginBatch() {
		aggregator.reset();
	}

	private void appendSequence(CompletableFuture<Void> sequenceFuture) {
		/*
		 * The sequence gets its failures reported independently:
		 * there's no need to propagate the failure to the executor.
		 * Also, we don't want later works to be cancelled just because this workset
		 * failed.
		 */
		future = sequenceFuture.exceptionally( e -> null );
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

		public void initSequence(CompletableFuture<?> previous) {
			sequenceBuilder.init( previous );
		}

		@Override
		public <T> CompletableFuture<T> addBulkable(BulkableElasticsearchWork<T> work) {
			return bulker.add( work );
		}

		@Override
		public <T> CompletableFuture<T> addNonBulkable(ElasticsearchWork<T> work) {
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
			return sequenceBuilder.build();
		}

		public void finalizeBulkWork() {
			bulker.finalizeBulkWork();
		}

		public void reset() {
			bulker.reset();
		}
	}
}