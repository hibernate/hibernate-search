/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkAggregator;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableWork;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;


/**
 * Executes works in parallel,
 * without any consideration for the order they were submitted in.
 * <p>
 * Two works submitted to this processor in the same batch will be executed
 * in the same bulk if possible, or in parallel if they can't be bulked.
 * <p>
 * This class is mutable and not thread-safe.
 */
class ElasticsearchParallelWorkProcessor implements ElasticsearchWorkProcessor {

	private final BulkAndSequenceAggregator aggregator;
	private final List<CompletableFuture<?>> sequenceFutures = new ArrayList<>();

	ElasticsearchParallelWorkProcessor(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.aggregator = new BulkAndSequenceAggregator( sequenceBuilder, bulker );
	}

	@Override
	public void beginBatch() {
		aggregator.reset();
		sequenceFutures.clear();
	}

	@Override
	public <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		aggregator.initSequence();
		CompletableFuture<T> future = work.aggregate( aggregator );
		sequenceFutures.add( aggregator.buildSequence() );
		return future;
	}

	@Override
	public CompletableFuture<Void> endBatch() {
		CompletableFuture<Void> future =
				CompletableFuture.allOf( sequenceFutures.toArray( new CompletableFuture<?>[0] ) );
		sequenceFutures.clear();
		aggregator.startSequences();
		// Sequence futures are not expected to fail even if one work fails,
		// so we can safely return this future directly.
		return future;
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
			this.sequenceBuilder = sequenceBuilder;
			this.bulker = bulker;
		}

		public void reset() {
			bulker.reset();
		}

		public void initSequence() {
			sequenceBuilder.init( CompletableFuture.completedFuture( null ) );
		}

		@Override
		public <T> CompletableFuture<T> addBulkable(BulkableWork<T> work) {
			return bulker.add( work );
		}

		@Override
		public <T> CompletableFuture<T> addNonBulkable(NonBulkableWork<T> work) {
			return sequenceBuilder.addNonBulkExecution( work );
		}

		public CompletableFuture<Void> buildSequence() {
			bulker.addWorksToSequence();
			return sequenceBuilder.build();
		}

		public void startSequences() {
			bulker.finalizeBulkWork();
		}

	}
}