/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;


/**
 * Aggregates works from changesets into a single sequence,
 * respecting the order they were submitted.
 * <p>
 * Two changesets or works submitted to this orchestrator will never run in parallel,
 * even if a reset() occurred between the two submissions.
 * <p>
 * This class is mutable and not thread-safe.
 *
 * @author Yoann Rodiere
 */
class SerialChangesetsElasticsearchWorkOrchestrator implements FlushableElasticsearchWorkOrchestrator {

	private final BulkAndSequenceAggregator aggregator;

	private CompletableFuture<Void> future = CompletableFuture.completedFuture( null );

	public SerialChangesetsElasticsearchWorkOrchestrator(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			ElasticsearchWorkBulker bulker) {
		this.aggregator = new BulkAndSequenceAggregator( sequenceBuilder, bulker );
	}

	@Override
	public CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> nonBulkedWorks) {
		aggregator.init( future );
		for ( ElasticsearchWork<?> work : nonBulkedWorks ) {
			work.aggregate( aggregator );
		}
		CompletableFuture<Void> sequenceFuture = aggregator.flushSequence();
		future = sequenceFuture;
		return sequenceFuture;
	}

	@Override
	public CompletableFuture<Void> flush() {
		aggregator.flushBulk();
		return future;
	}

	@Override
	public void reset() {
		aggregator.reset();
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

		public void init(CompletableFuture<?> previous) {
			sequenceBuilder.init( previous );
		}

		@Override
		public void addBulkable(BulkableElasticsearchWork<?> work) {
			bulker.add( work );
		}

		@Override
		public void addNonBulkable(ElasticsearchWork<?> work) {
			if ( bulker.flushBulked() ) {
				/*
				 * We want to execute works in the exact order they were received,
				 * so if a non-bulkable work is about to be added,
				 * we can't add any more works to the current bulk
				 * (otherwise the next bulked works may be executed
				 * before the current non-bulkable work).
				 */
				bulker.flushBulk();
			}
			sequenceBuilder.addNonBulkExecution( work );
		}

		public CompletableFuture<Void> flushSequence() {
			bulker.flushBulked();
			return sequenceBuilder.build();
		}

		public void flushBulk() {
			bulker.flushBulk();
		}

		public void reset() {
			bulker.reset();
		}
	}
}