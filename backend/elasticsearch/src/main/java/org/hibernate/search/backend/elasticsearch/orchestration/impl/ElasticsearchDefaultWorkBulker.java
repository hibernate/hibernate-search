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
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.Futures;

class ElasticsearchDefaultWorkBulker implements ElasticsearchWorkBulker {

	private final ElasticsearchWorkSequenceBuilder sequenceBuilder;
	private final Function<List<? extends BulkableElasticsearchWork<?>>, ElasticsearchWork<BulkResult>> bulkWorkFactory;
	private final int minBulkSize;
	private final int maxBulkSize;

	private final List<BulkableElasticsearchWork<?>> currentBulkItems;
	private final List<CompletableFuture<?>> currentBulkItemsFutures;
	private int currentBulkFirstUnflushedItem;
	private CompletableFuture<ElasticsearchWork<BulkResult>> currentBulkWorkFuture;
	private CompletableFuture<BulkResult> currentBulkResultFuture;

	/**
	 * @param sequenceBuilder The sequence builder to send flushed works to
	 * @param bulkWorkFactory The factory for bulk works
	 * @param minBulkSize Minimum number of works in a single bulk.
	 * If a {@link #flushBulked() flush of bulked works} is requested before
	 * this threshold has been reached, works will not be bulked.
	 * @param maxBulkSize Maximum number of works in a single bulk.
	 * If a bulk reaches this size, it will be automatically {@link #flushBulk() flushed}
	 * to the underlying sequence builder.
	 */
	public ElasticsearchDefaultWorkBulker(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			Function<List<? extends BulkableElasticsearchWork<?>>, ElasticsearchWork<BulkResult>> bulkWorkFactory,
			int minBulkSize, int maxBulkSize) {
		this.sequenceBuilder = sequenceBuilder;
		this.bulkWorkFactory = bulkWorkFactory;
		this.minBulkSize = minBulkSize;
		this.maxBulkSize = maxBulkSize;

		this.currentBulkItems = new ArrayList<>();
		this.currentBulkItemsFutures = new ArrayList<>();
		this.currentBulkFirstUnflushedItem = 0;
		this.currentBulkWorkFuture = null;
		this.currentBulkResultFuture = null;
	}

	@Override
	public <T> CompletableFuture<T> add(BulkableElasticsearchWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		currentBulkItems.add( work );
		currentBulkItemsFutures.add( future );
		if ( currentBulkItems.size() >= maxBulkSize ) {
			flushBulked();
			flushBulk();
		}
		return future;
	}

	@Override
	public boolean flushBulked() {
		int currentBulkWorksSize = currentBulkItems.size();
		if ( currentBulkWorksSize <= currentBulkFirstUnflushedItem ) {
			// No work to flush
			return false;
		}
		else if ( currentBulkWorksSize < minBulkSize && currentBulkFirstUnflushedItem == 0 ) {
			/*
			 * Not enough works in the bulk, and no work has been flushed yet.
			 * We'll just flush the works without bulking them,
			 * and start a new bulk.
			 */
			for ( int i = 0; i < currentBulkWorksSize ; ++i ) {
				BulkableElasticsearchWork<?> work = currentBulkItems.get( i );
				addAndConnectNonBulkedWorkExecution( work, i );
			}
			reset();
			return false;
		}

		if ( currentBulkWorkFuture == null ) {
			currentBulkWorkFuture = new CompletableFuture<>();
			currentBulkResultFuture = sequenceBuilder.addBulkExecution( currentBulkWorkFuture );
		}

		BulkResultExtractionStep extractionStep = sequenceBuilder.addBulkResultExtraction( currentBulkResultFuture );
		for ( int i = currentBulkFirstUnflushedItem; i < currentBulkWorksSize ; ++i ) {
			BulkableElasticsearchWork<?> work = currentBulkItems.get( i );
			addAndConnectBulkedWorkExtraction( extractionStep, work, i );
		}
		currentBulkFirstUnflushedItem = currentBulkWorksSize;

		return true;
	}

	@Override
	public void flushBulk() {
		if ( currentBulkItems.size() != currentBulkFirstUnflushedItem ) {
			throw new AssertionFailure( "Some works haven't been flushed to the sequence builder" );
		}

		if ( currentBulkWorkFuture == null ) {
			// No work was bulked, so there's nothing to do
			return;
		}

		ElasticsearchWork<BulkResult> bulkWork = bulkWorkFactory.apply( currentBulkItems );
		currentBulkWorkFuture.complete( bulkWork );
		reset();
	}

	@Override
	public void reset() {
		this.currentBulkItems.clear();
		this.currentBulkItemsFutures.clear();
		this.currentBulkFirstUnflushedItem = 0;
		this.currentBulkWorkFuture = null;
		this.currentBulkResultFuture = null;
	}

	private <T> void addAndConnectNonBulkedWorkExecution(BulkableElasticsearchWork<T> work, int index) {
		@SuppressWarnings("unchecked") // The type T of the future matches the one of the work with the same index; see add()
		CompletableFuture<T> future = (CompletableFuture<T>) currentBulkItemsFutures.get( index );
		sequenceBuilder.addNonBulkExecution( work )
				.whenComplete( Futures.copyHandler( future ) );
	}

	private <T> void addAndConnectBulkedWorkExtraction(BulkResultExtractionStep extractionStep,
			BulkableElasticsearchWork<T> work, int index) {
		@SuppressWarnings("unchecked") // The type T of the future matches the one of the work with the same index; see add()
		CompletableFuture<T> future = (CompletableFuture<T>) currentBulkItemsFutures.get( index );
		extractionStep.add( work, index )
				.whenComplete( Futures.copyHandler( future ) );
	}
}
