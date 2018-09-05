/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;


/**
 * Aggregates works from changesets into multiple sequences.
 * <p>
 * Two works will be executed sequentially if they are part of the same changeset.
 * Two works from different changesets will be executed in parallel.
 * <p>
 * This class is mutable and not thread-safe.
 *
 * @author Yoann Rodiere
 */
class ParallelChangesetsElasticsearchWorkOrchestrator implements FlushableElasticsearchWorkOrchestrator {

	private final BulkAndSequenceAggregator aggregator;
	private final List<CompletableFuture<?>> sequenceFutures = new ArrayList<>();

	public ParallelChangesetsElasticsearchWorkOrchestrator(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.aggregator = new BulkAndSequenceAggregator( sequenceBuilder, bulker );
	}

	@Override
	public CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> nonBulkedWorks) {
		aggregator.initSequence();
		for ( ElasticsearchWork<?> work : nonBulkedWorks ) {
			work.aggregate( aggregator );
		}
		CompletableFuture<Void> future = aggregator.flushSequence();
		sequenceFutures.add( future );
		return future;
	}

	@Override
	public CompletableFuture<Void> flush() {
		CompletableFuture<Void> future =
				CompletableFuture.allOf( sequenceFutures.toArray( new CompletableFuture[ sequenceFutures.size()] ) );
		sequenceFutures.clear();
		aggregator.startSequences();
		return future;
	}

	@Override
	public void reset() {
		aggregator.reset();
		sequenceFutures.clear();
	}

	private static class BulkAndSequenceAggregator implements ElasticsearchWorkAggregator {

		private final ElasticsearchWorkSequenceBuilder sequenceBuilder;
		private final ElasticsearchWorkBulker bulker;

		private CompletableFuture<Void> rootFuture;
		private boolean currentBulkIsUsableInSameSequence = true;

		public BulkAndSequenceAggregator(ElasticsearchWorkSequenceBuilder sequenceBuilder,
				ElasticsearchWorkBulker bulker) {
			super();
			this.rootFuture = CompletableFuture.completedFuture( null );
			this.sequenceBuilder = sequenceBuilder;
			this.bulker = bulker;
		}

		public void initSequence() {
			sequenceBuilder.init( rootFuture );
		}

		@Override
		public void addBulkable(BulkableElasticsearchWork<?> work) {
			if ( !currentBulkIsUsableInSameSequence ) {
				bulker.flushBulk();
				currentBulkIsUsableInSameSequence = true;
			}
			bulker.add( work );
		}

		@Override
		public void addNonBulkable(ElasticsearchWork<?> work) {
			if ( bulker.flushBulked() ) {
				/*
				 * A non-bulkable work follows bulked works,
				 * so we won't be able to re-use the same bulk in the same sequence
				 * (otherwise the relative order of works would be altered).
				 */
				currentBulkIsUsableInSameSequence = false;
			}
			sequenceBuilder.addNonBulkExecution( work );
		}

		public CompletableFuture<Void> flushSequence() {
			bulker.flushBulked();
			CompletableFuture<Void> future = sequenceBuilder.build();
			currentBulkIsUsableInSameSequence = true;
			return future;
		}

		public void startSequences() {
			bulker.flushBulk();
		}

		public void reset() {
			bulker.reset();
			rootFuture = CompletableFuture.completedFuture( null );
			sequenceBuilder.init( rootFuture );
		}
	}
}