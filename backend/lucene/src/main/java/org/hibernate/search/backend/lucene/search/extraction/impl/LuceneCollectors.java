/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.ChildrenCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
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
import org.apache.lucene.search.TotalHitCountCollector;

public class LuceneCollectors {

	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private final TopDocsCollector<?> topDocsCollector;
	private final TotalHitCountCollector totalHitCountCollector;
	private final ChildrenCollector childrenCollector;

	private final Collector compositeCollector;
	private final Map<CollectorKey<?>, Collector> collectors;

	private final TimeoutManager timeoutManager;

	private TopDocs topDocs = null;
	private Map<Integer, Set<Integer>> topDocIdsToNestedDocIds = Collections.emptyMap();

	LuceneCollectors(IndexSearcher indexSearcher, Query luceneQuery,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			TopDocsCollector<?> topDocsCollector,
			TotalHitCountCollector totalHitCountCollector, ChildrenCollector childrenCollector,
			Collector compositeCollector,
			Map<CollectorKey<?>, Collector> collectors,
			TimeoutManager timeoutManager) {
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.topDocsCollector = topDocsCollector;
		this.totalHitCountCollector = totalHitCountCollector;
		this.childrenCollector = childrenCollector;
		this.compositeCollector = compositeCollector;
		this.collectors = collectors;
		this.timeoutManager = timeoutManager;
	}

	public void collect(int offset, Integer limit) throws IOException {
		if ( timeoutManager.checkTimedOut() ) {
			// in case of timeout before the query execution, skip the query
			return;
		}

		try {
			indexSearcher.search( luceneQuery, compositeCollector );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}
		if ( topDocsCollector == null ) {
			return;
		}

		extractTopDocs( offset, limit );
		if ( requireFieldDocRescoring ) {
			handleRescoring( indexSearcher, luceneQuery );
		}

		if ( childrenCollector != null ) {
			this.topDocIdsToNestedDocIds = childrenCollector.getChildren();
		}
	}

	public Map<CollectorKey<?>, Collector> getCollectors() {
		return collectors;
	}

	public long getTotalHits() {
		return totalHitCountCollector.getTotalHits();
	}

	public TopDocs getTopDocs() {
		return topDocs;
	}

	public Map<Integer, Set<Integer>> getTopDocIdsToNestedDocIds() {
		return topDocIdsToNestedDocIds;
	}

	private void extractTopDocs(int offset, Integer limit) {
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
}
