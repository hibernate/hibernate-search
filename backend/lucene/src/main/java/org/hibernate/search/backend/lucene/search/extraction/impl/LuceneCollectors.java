/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;

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

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private TopDocs topDocs = null;

	LuceneCollectors(TopDocsCollector<?> topDocsCollector, TotalHitCountCollector totalHitCountCollector,
			Collector compositeCollector,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring) {
		this.topDocsCollector = topDocsCollector;
		this.totalHitCountCollector = totalHitCountCollector;
		this.compositeCollector = compositeCollector;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
	}

	public void collect(IndexSearcher indexSearcher, Query luceneQuery, long offset, Long limit) throws IOException {
		indexSearcher.search( luceneQuery, compositeCollector );

		if ( topDocsCollector != null ) {
			if ( limit == null ) {
				topDocs = topDocsCollector.topDocs( (int) offset );
			}
			else {
				topDocs = topDocsCollector.topDocs( (int) offset, limit.intValue() );
			}

			if ( requireFieldDocRescoring ) {
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
		else {
			topDocs = null;
		}
	}

	public long getTotalHits() {
		return totalHitCountCollector.getTotalHits();
	}

	public TopDocs getTopDocs() {
		return topDocs;
	}
}
