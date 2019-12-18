/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.HibernateSearchDocumentIdToLuceneDocIdMapCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.LuceneCollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.LuceneChildrenCollector;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.BooleanQuery;
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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;
	private final Set<String> nestedDocumentPaths;

	private final TopDocsCollector<?> topDocsCollector;
	private final TotalHitCountCollector totalHitCountCollector;
	private final LuceneChildrenCollector childrenCollector;

	private final Collector compositeCollector;
	private final Collector compositeCollectorForNestedDocuments;
	private final Map<LuceneCollectorKey<?>, Collector> collectors;

	private final TimeoutManager timeoutManager;

	private TopDocs topDocs = null;
	private Map<Integer, Set<Integer>> topDocIdsToNestedDocIds = Collections.emptyMap();

	LuceneCollectors(boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			Set<String> nestedDocumentPaths,
			TopDocsCollector<?> topDocsCollector,
			TotalHitCountCollector totalHitCountCollector, LuceneChildrenCollector childrenCollector,
			Collector compositeCollector, Collector compositeCollectorForNestedDocuments,
			Map<LuceneCollectorKey<?>, Collector> collectors,
			TimeoutManager timeoutManager) {
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.nestedDocumentPaths = nestedDocumentPaths;
		this.topDocsCollector = topDocsCollector;
		this.totalHitCountCollector = totalHitCountCollector;
		this.childrenCollector = childrenCollector;
		this.compositeCollector = compositeCollector;
		this.compositeCollectorForNestedDocuments = compositeCollectorForNestedDocuments;
		this.collectors = collectors;
		this.timeoutManager = timeoutManager;
	}

	public void collect(IndexSearcher indexSearcher, Query luceneQuery, int offset, Integer limit) throws IOException {
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

		if ( timeoutManager.isTimedOut() ) {
			return; // Skip nested docs
		}

		try {
			collectNestedDocs( indexSearcher, luceneQuery );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}
	}

	public Map<LuceneCollectorKey<?>, Collector> getCollectors() {
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

	private void collectNestedDocs(IndexSearcher indexSearcher, Query parentsQuery) {
		// if the projection does not need any nested object skip their fetching
		if ( topDocs == null || nestedDocumentPaths.isEmpty() ) {
			return;
		}

		HibernateSearchDocumentIdToLuceneDocIdMapCollector searchDocumentIdToLuceneDocId =
				(HibernateSearchDocumentIdToLuceneDocIdMapCollector)
						collectors.get( HibernateSearchDocumentIdToLuceneDocIdMapCollector.FACTORY );

		Map<String, Set<Integer>> stringSetMap = applyCollectorsToNestedDocs( indexSearcher, parentsQuery );
		this.topDocIdsToNestedDocIds = new HashMap<>();
		for ( Map.Entry<String, Set<Integer>> entry : stringSetMap.entrySet() ) {
			topDocIdsToNestedDocIds.put( searchDocumentIdToLuceneDocId.getLuceneDocId( entry.getKey() ), entry.getValue() );
		}
	}

	private Map<String, Set<Integer>> applyCollectorsToNestedDocs(IndexSearcher indexSearcher, Query parentsQuery) {
		BooleanQuery booleanQuery = LuceneQueries.findChildQuery( nestedDocumentPaths, parentsQuery );

		try {
			indexSearcher.search( booleanQuery, compositeCollectorForNestedDocuments );
			return childrenCollector.getChildren();
		}
		catch (IOException e) {
			throw log.errorFetchingNestedDocuments( booleanQuery, e );
		}
	}
}
