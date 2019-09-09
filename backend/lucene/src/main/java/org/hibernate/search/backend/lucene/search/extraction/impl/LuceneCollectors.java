/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.types.sort.nested.impl.NestedFieldComparatorSource;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TotalHitCountCollector;

public class LuceneCollectors {

	private final TopDocsCollector<?> topDocsCollector;

	private final TotalHitCountCollector totalHitCountCollector;

	private final Collector compositeCollector;
	private final Collection<Collector> collectorsForNestedDocuments;

	private final Map<LuceneCollectorKey<?>, Collector> collectors;

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private final List<NestedFieldComparatorSource> nestedFieldSorts;

	private TopDocs topDocs = null;

	LuceneCollectors(TopDocsCollector<?> topDocsCollector, TotalHitCountCollector totalHitCountCollector,
			Collector compositeCollector, Collection<Collector> collectorsForNestedDocuments,
			Map<LuceneCollectorKey<?>, Collector> collectors,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			List<NestedFieldComparatorSource> nestedFieldSorts) {
		this.topDocsCollector = topDocsCollector;
		this.totalHitCountCollector = totalHitCountCollector;
		this.compositeCollector = compositeCollector;
		this.collectorsForNestedDocuments = collectorsForNestedDocuments;
		this.collectors = collectors;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.nestedFieldSorts = nestedFieldSorts;
	}

	public void collect(IndexSearcher indexSearcher, Query luceneQuery, int offset, Integer limit) throws IOException {
		if ( nestedFieldSorts != null ) {
			for ( NestedFieldComparatorSource nestedField : nestedFieldSorts ) {
				nestedField.setOriginalParentQuery( luceneQuery );
			}
		}
		indexSearcher.search( luceneQuery, compositeCollector );

		if ( topDocsCollector == null ) {
			return;
		}
		extractTopDocs( offset, limit );
		if ( requireFieldDocRescoring ) {
			handleRescoring( indexSearcher, luceneQuery );
		}
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

	public Collection<Collector> getCollectorsForNestedDocuments() {
		return collectorsForNestedDocuments;
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
}
