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
import java.util.function.BiFunction;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.backend.elasticsearch.work.result.impl.BulkResult;
import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

class ElasticsearchDefaultWorkBulker implements ElasticsearchWorkBulker {

	private final ElasticsearchWorkSequenceBuilder sequenceBuilder;
	private final BiFunction<List<? extends BulkableElasticsearchWork<?>>, DocumentRefreshStrategy, ElasticsearchWork<BulkResult>> bulkWorkFactory;
	private final int minBulkSize;
	private final int maxBulkSize;

	private final List<BulkableElasticsearchWork<?>> currentBulkItems;
	private final List<CompletableFuture<?>> currentBulkItemsFutures;
	private int currentBulkFirstNonAddedItem;
	private DocumentRefreshStrategy currentBulkRefreshStrategy;
	private CompletableFuture<ElasticsearchWork<BulkResult>> currentBulkWorkFuture;
	private CompletableFuture<BulkResult> currentBulkResultFuture;

	/**
	 * @param sequenceBuilder The sequence builder to add works to
	 * @param bulkWorkFactory The factory for bulk works
	 * @param minBulkSize Minimum number of works in a single bulk.
	 * If {@link #addWorksToSequence() adding works to the sequence} is requested before
	 * this threshold has been reached, works will not be bulked.
	 * @param maxBulkSize Maximum number of works in a single bulk.
	 * If a bulk reaches this size, it will be automatically
	 * {@link #addWorksToSequence() add the bulk work and work extractions to the sequence}
	 * and {@link #finalizeBulkWork() finalize the bulk work}
	 * to the underlying sequence builder.
	 */
	public ElasticsearchDefaultWorkBulker(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			BiFunction<List<? extends BulkableElasticsearchWork<?>>, DocumentRefreshStrategy, ElasticsearchWork<BulkResult>> bulkWorkFactory,
			int minBulkSize, int maxBulkSize) {
		this.sequenceBuilder = sequenceBuilder;
		this.bulkWorkFactory = bulkWorkFactory;
		this.minBulkSize = minBulkSize;
		this.maxBulkSize = maxBulkSize;

		this.currentBulkItems = new ArrayList<>();
		this.currentBulkItemsFutures = new ArrayList<>();
		this.currentBulkFirstNonAddedItem = 0;
		this.currentBulkWorkFuture = null;
		this.currentBulkResultFuture = null;
	}

	@Override
	public <T> CompletableFuture<T> add(BulkableElasticsearchWork<T> work) {
		DocumentRefreshStrategy workRefreshStrategy = work.getRefreshStrategy();
		if ( currentBulkItems.isEmpty() ) {
			currentBulkRefreshStrategy = workRefreshStrategy;
		}
		else if ( currentBulkRefreshStrategy != workRefreshStrategy ) {
			// This work needs a bulk with a different "refresh" parameter; we can't reuse the current bulk.
			addWorksToSequence();
			finalizeBulkWork();
			currentBulkRefreshStrategy = workRefreshStrategy;
		}

		CompletableFuture<T> future = new CompletableFuture<>();
		currentBulkItems.add( work );
		currentBulkItemsFutures.add( future );
		if ( currentBulkItems.size() >= maxBulkSize ) {
			addWorksToSequence();
			finalizeBulkWork();
		}
		return future;
	}

	@Override
	public boolean addWorksToSequence() {
		int currentBulkWorksSize = currentBulkItems.size();
		if ( currentBulkWorksSize <= currentBulkFirstNonAddedItem ) {
			// No work to add
			return false;
		}
		else if ( currentBulkWorksSize < minBulkSize && currentBulkFirstNonAddedItem == 0 ) {
			/*
			 * Not enough works in the bulk, and no work has been added to the sequence yet.
			 * We'll just add the works to the sequence without bulking them,
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
		for ( int i = currentBulkFirstNonAddedItem; i < currentBulkWorksSize ; ++i ) {
			BulkableElasticsearchWork<?> work = currentBulkItems.get( i );
			addAndConnectBulkedWorkExtraction( extractionStep, work, i );
		}
		currentBulkFirstNonAddedItem = currentBulkWorksSize;

		return true;
	}

	@Override
	public void finalizeBulkWork() {
		if ( currentBulkItems.size() != currentBulkFirstNonAddedItem ) {
			throw new AssertionFailure( "Some works haven't been added to the sequence builder" );
		}

		if ( currentBulkWorkFuture == null ) {
			// No work was bulked, so there's nothing to do
			return;
		}

		ElasticsearchWork<BulkResult> bulkWork = bulkWorkFactory.apply( currentBulkItems, currentBulkRefreshStrategy );
		currentBulkWorkFuture.complete( bulkWork );
		reset();
	}

	@Override
	public void reset() {
		this.currentBulkItems.clear();
		this.currentBulkItemsFutures.clear();
		this.currentBulkFirstNonAddedItem = 0;
		this.currentBulkRefreshStrategy = null;
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
