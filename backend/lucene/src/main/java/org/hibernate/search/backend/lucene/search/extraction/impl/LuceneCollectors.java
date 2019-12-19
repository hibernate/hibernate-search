/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.ExplicitDocIdsQuery;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.Weight;

public class LuceneCollectors {

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private final CollectorSet allMatchingDocsCollectors;
	private final CollectorSet topDocsCollectors;

	private final TimeoutManager timeoutManager;

	private long totalHitCount = 0;
	private TopDocs topDocs = null;

	LuceneCollectors(IndexSearcher indexSearcher, Query luceneQuery,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			CollectorSet allMatchingDocsCollectors, CollectorSet topDocsCollectors,
			TimeoutManager timeoutManager) {
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.allMatchingDocsCollectors = allMatchingDocsCollectors;
		this.topDocsCollectors = topDocsCollectors;
		this.timeoutManager = timeoutManager;
	}

	public void collect(int offset, Integer limit) throws IOException {
		if ( timeoutManager.checkTimedOut() ) {
			// in case of timeout before the query execution, skip the query
			return;
		}

		// Phase 1: collect top docs and aggregations
		try {
			indexSearcher.search( luceneQuery, allMatchingDocsCollectors.getComposed() );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}

		this.totalHitCount = allMatchingDocsCollectors.get( CollectorKey.TOTAL_HIT_COUNT ).getTotalHits();

		TopDocsCollector<?> topDocsCollector = allMatchingDocsCollectors.get( CollectorKey.TOP_DOCS );
		if ( topDocsCollector == null ) {
			return;
		}

		extractTopDocs( topDocsCollector, offset, limit );
		if ( requireFieldDocRescoring ) {
			handleRescoring( indexSearcher, luceneQuery );
		}

		// Phase 2: apply collectors to top docs
		if ( topDocsCollectors == null ) {
			return;
		}
		try {
			applyCollectorsToTopDocs();
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}
	}

	public CollectorSet getAllMatchingDocsCollectors() {
		return allMatchingDocsCollectors;
	}

	public CollectorSet getTopDocsCollectors() {
		return topDocsCollectors;
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
		Weight weight = indexSearcher.createWeight( topDocsQuery, ScoreMode.COMPLETE_NO_SCORES, 1.0f );

		Collector collector = topDocsCollectors.getComposed();

		for ( LeafReaderContext ctx : indexSearcher.getTopReaderContext().leaves() ) {
			final LeafCollector leafCollector;
			try {
				leafCollector = collector.getLeafCollector( ctx );
			}
			catch (CollectionTerminatedException e) {
				// there is no doc of interest in this reader context
				// continue with the following leaf
				continue;
			}
			BulkScorer scorer = weight.bulkScorer( ctx );
			if ( scorer != null ) {
				try {
					scorer.score( leafCollector, ctx.reader().getLiveDocs() );
				}
				catch (CollectionTerminatedException e) {
					// collection was terminated prematurely
					// continue with the following leaf
				}
			}
		}
	}
}
