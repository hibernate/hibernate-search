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
import java.util.function.Function;

import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkSequenceBuilder.BulkResultExtractionStep;
import org.hibernate.search.elasticsearch.work.impl.BulkResult;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.exception.AssertionFailure;

class DefaultElasticsearchWorkBulker implements ElasticsearchWorkBulker {

	/**
	 * Maximum number of requests sent in a single bulk. Could be made an option if needed.
	 */
	static final int MAX_BULK_SIZE = 250;

	private final ElasticsearchWorkSequenceBuilder sequenceBuilder;
	private final Function<List<BulkableElasticsearchWork<?>>, ElasticsearchWork<BulkResult>> bulkWorkFactory;

	private final List<BulkableElasticsearchWork<?>> currentBulkWorks;
	private int currentBulkFirstUnflushedWork;
	private CompletableFuture<ElasticsearchWork<BulkResult>> currentBulkWorkFuture;
	private CompletableFuture<BulkResult> currentBulkResultFuture;

	public DefaultElasticsearchWorkBulker(ElasticsearchWorkSequenceBuilder sequenceBuilder,
			Function<List<BulkableElasticsearchWork<?>>, ElasticsearchWork<BulkResult>> bulkWorkFactory) {
		this.sequenceBuilder = sequenceBuilder;
		this.bulkWorkFactory = bulkWorkFactory;

		this.currentBulkWorks = new ArrayList<>();
		this.currentBulkFirstUnflushedWork = 0;
		this.currentBulkWorkFuture = null;
		this.currentBulkResultFuture = null;
	}

	@Override
	public void add(BulkableElasticsearchWork<?> work) {
		currentBulkWorks.add( work );
		if ( currentBulkWorks.size() >= MAX_BULK_SIZE ) {
			flushBulked();
			flushBulk();
		}
	}

	@Override
	public boolean flushBulked() {
		int currentBulkWorksSize = currentBulkWorks.size();
		if ( currentBulkWorksSize <= currentBulkFirstUnflushedWork ) {
			// No work to flush
			return false;
		}
		else if ( currentBulkWorksSize == 1 ) {
			/*
			 * Only one work in the bulk, and it hasn't been flushed yet.
			 * We'll just flush it without bulking it,
			 * and start a new bulk, to avoid single-element bulks.
			 */
			ElasticsearchWork<?> work = currentBulkWorks.iterator().next();
			sequenceBuilder.addNonBulkExecution( work );
			reset();
			return false;
		}

		if ( currentBulkWorkFuture == null ) {
			currentBulkWorkFuture = new CompletableFuture<>();
			currentBulkResultFuture = sequenceBuilder.addBulkExecution( currentBulkWorkFuture );
		}

		BulkResultExtractionStep extractionStep = sequenceBuilder.startBulkResultExtraction( currentBulkResultFuture );
		for ( int i = currentBulkFirstUnflushedWork ; i < currentBulkWorksSize ; ++i ) {
			int bulkedWorkIndex = i;
			BulkableElasticsearchWork<?> bulkedWork = currentBulkWorks.get( bulkedWorkIndex );
			extractionStep.add( bulkedWork, bulkedWorkIndex );
		}
		currentBulkFirstUnflushedWork = currentBulkWorksSize;

		return true;
	}

	@Override
	public void flushBulk() {
		if ( currentBulkWorks.size() != currentBulkFirstUnflushedWork ) {
			throw new AssertionFailure( "Some works haven't been flushed to the sequence builder" );
		}

		if ( currentBulkWorkFuture == null ) {
			// No work was bulked, so there's nothing to do
			return;
		}

		ElasticsearchWork<BulkResult> bulkWork = bulkWorkFactory.apply( currentBulkWorks );
		currentBulkWorkFuture.complete( bulkWork );
		reset();
	}

	@Override
	public void reset() {
		this.currentBulkWorks.clear();
		this.currentBulkFirstUnflushedWork = 0;
		this.currentBulkWorkFuture = null;
		this.currentBulkResultFuture = null;
	}
}
