/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.ExplicitDocIdsQuery;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;

public class LuceneCollectors {

	private final IndexReaderMetadataResolver metadataResolver;

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private final CollectorSet collectorsForAllMatchingDocs;
	private final Set<CollectorFactory<?>> collectorsForTopDocsFactories;
	private CollectorSet collectorsForTopDocs;

	private final TimeoutManager timeoutManager;

	private long totalHitCount = 0;
	private TopDocs topDocs = null;

	LuceneCollectors(IndexReaderMetadataResolver metadataResolver, IndexSearcher indexSearcher, Query luceneQuery,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			CollectorSet collectorsForAllMatchingDocs,
			Set<CollectorFactory<?>> collectorsForTopDocsFactories,
			TimeoutManager timeoutManager) {
		this.metadataResolver = metadataResolver;
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.collectorsForAllMatchingDocs = collectorsForAllMatchingDocs;
		this.collectorsForTopDocsFactories = collectorsForTopDocsFactories;
		this.timeoutManager = timeoutManager;
	}

	public void collect(int offset, Integer limit) throws IOException {
		if ( timeoutManager.checkTimedOut() ) {
			// in case of timeout before the query execution, skip the query
			return;
		}

		// Phase 1: collect top docs and aggregations
		try {
			indexSearcher.search( luceneQuery, collectorsForAllMatchingDocs.getComposed() );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}

		this.totalHitCount = collectorsForAllMatchingDocs.get( CollectorKey.TOTAL_HIT_COUNT ).getTotalHits();

		TopDocsCollector<?> topDocsCollector = collectorsForAllMatchingDocs.get( CollectorKey.TOP_DOCS );
		if ( topDocsCollector == null ) {
			return;
		}

		extractTopDocs( topDocsCollector, offset, limit );
		if ( requireFieldDocRescoring ) {
			handleRescoring( indexSearcher, luceneQuery );
		}

		// Phase 2: apply collectors to top docs
		if ( collectorsForTopDocsFactories.isEmpty() ) {
			return;
		}
		try {
			applyCollectorsToTopDocs();
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}
	}

	public CollectorSet getCollectorsForAllMatchingDocs() {
		return collectorsForAllMatchingDocs;
	}

	public CollectorSet getCollectorsForTopDocs() {
		return collectorsForTopDocs;
	}

	public long getTotalHitCount() {
		return totalHitCount;
	}

	public TopDocs getTopDocs() {
		return topDocs;
	}

	private void extractTopDocs(TopDocsCollector<?> topDocsCollector, int offset, Integer limit) {
		if ( limit == null ) {
			topDocs = topDocsCollector.topDocs( offset );
		}
		else {
			topDocs = topDocsCollector.topDocs( offset, limit );
		}
	}

	private void handleRescoring(IndexSearcher indexSearcher, Query luceneQuery) throws IOException {
		if ( scoreSortFieldIndexForRescoring != null ) {
			// If there's a SCORE sort field, just get the score value from the sort field
			for ( ScoreDoc scoreDoc : topDocs.scoreDocs ) {
				FieldDoc fieldDoc = (FieldDoc) scoreDoc;
				fieldDoc.score = (float) fieldDoc.fields[scoreSortFieldIndexForRescoring];
			}
		}
		else {
			// Failing that, we need to re-score the top documents after the query was executed.
			// This feels wrong, but apparently that's the recommended practice...
			TopFieldCollector.populateScores( topDocs.scoreDocs, indexSearcher, luceneQuery );
		}
	}

	private void applyCollectorsToTopDocs() throws IOException {
		ExplicitDocIdsQuery topDocsQuery = new ExplicitDocIdsQuery( topDocs.scoreDocs );

		CollectorExecutionContext executionContext = new CollectorExecutionContext(
				metadataResolver, indexSearcher,
				// Only join nested documents for the top documents (not for all documents matching this.luceneQuery).
				topDocsQuery,
				// Allocate just enough memory to handle the top documents.
				topDocs.scoreDocs.length
		);

		CollectorSet.Builder collectorForTopDocsBuilder =
				new CollectorSet.Builder( executionContext, timeoutManager );
		collectorForTopDocsBuilder.addAll( collectorsForTopDocsFactories );
		this.collectorsForTopDocs = collectorForTopDocsBuilder.build();

		Collector collector = this.collectorsForTopDocs.getComposed();

		// This will collect data
		indexSearcher.search( topDocsQuery, collector );
	}
}
