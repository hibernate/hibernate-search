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
import org.hibernate.search.backend.lucene.search.timeout.impl.LuceneTimeoutManager;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TotalHitCountCollector;

public class LuceneCollectors {

	static final CollectorKey<TotalHitCountCollector> TOTAL_HIT_COUNT_KEY = CollectorKey.create();
	static final CollectorKey<TopDocsCollector<?>> TOP_DOCS_KEY = CollectorKey.create();

	private final IndexReaderMetadataResolver metadataResolver;

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private final CollectorSet collectorsForAllMatchingDocs;
	private final Set<CollectorFactory<?>> collectorsForTopDocsFactories;
	private CollectorSet collectorsForTopDocs;

	private final LuceneTimeoutManager timeoutManager;

	private Long totalHitCount;
	private TopDocs topDocs = null;

	LuceneCollectors(IndexReaderMetadataResolver metadataResolver, IndexSearcher indexSearcher, Query luceneQuery,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			CollectorSet collectorsForAllMatchingDocs,
			Set<CollectorFactory<?>> collectorsForTopDocsFactories,
			LuceneTimeoutManager timeoutManager) {
		this.metadataResolver = metadataResolver;
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.collectorsForAllMatchingDocs = collectorsForAllMatchingDocs;
		this.collectorsForTopDocsFactories = collectorsForTopDocsFactories;
		this.timeoutManager = timeoutManager;
	}

	/**
	 * Phase 1: collect matching docs.
	 * Collects the total hit count, aggregations, and top docs.
	 *
	 * @param offset The index of the first collected top doc.
	 * @param limit The maximum amount of top docs to collect.
	 * @throws IOException If Lucene throws an {@link IOException}.
	 */
	public void collectMatchingDocs(int offset, Integer limit) throws IOException {
		if ( timeoutManager.checkTimedOut() ) {
			// in case of timeout before the query execution, skip the query
			return;
		}

		// Phase 1: collect top docs and aggregations
		try {
			Collector composed = collectorsForAllMatchingDocs.getComposed();
			if ( composed != null ) {
				indexSearcher.search( luceneQuery, composed );
			}
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}

		TotalHitCountCollector totalHitCountCollector = collectorsForAllMatchingDocs.get( TOTAL_HIT_COUNT_KEY );
		if ( totalHitCountCollector != null ) {
			totalHitCount = Long.valueOf( totalHitCountCollector.getTotalHits() );
		}

		TopDocsCollector<?> topDocsCollector = collectorsForAllMatchingDocs.get( TOP_DOCS_KEY );
		if ( topDocsCollector == null ) {
			return;
		}

		extractTopDocs( topDocsCollector, offset, limit );
		if ( requireFieldDocRescoring ) {
			handleRescoring( indexSearcher, luceneQuery );
		}
	}

	public CollectorSet getCollectorsForAllMatchingDocs() {
		return collectorsForAllMatchingDocs;
	}

	/**
	 * Phase 2: collect data relative to top docs.
	 *
	 * @param startInclusive The index of the first top doc whose data to collect.
	 * @param endExclusive The index after the last top doc whose data to collect.
	 *
	 * @throws IOException If Lucene throws an {@link IOException}.
	 */
	public void collectTopDocsData(int startInclusive, int endExclusive) throws IOException {
		if ( collectorsForTopDocsFactories.isEmpty() || topDocs == null ) {
			this.collectorsForTopDocs = null;
			return;
		}
		try {
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			ExplicitDocIdsQuery topDocsQuery = new ExplicitDocIdsQuery( scoreDocs, startInclusive, endExclusive );
			this.collectorsForTopDocs = buildTopdDocsDataCollectors( topDocsQuery );
			indexSearcher.search( topDocsQuery, collectorsForTopDocs.getComposed() );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}
	}

	public CollectorSet getCollectorsForTopDocs() {
		return collectorsForTopDocs;
	}

	public Long getTotalHitCount() {
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

	private CollectorSet buildTopdDocsDataCollectors(Query topDocsQuery) throws IOException {
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
		return collectorForTopDocsBuilder.build();
	}
}
