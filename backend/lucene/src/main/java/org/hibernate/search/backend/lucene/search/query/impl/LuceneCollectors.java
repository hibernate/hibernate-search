/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TotalHitCountCollector;

class LuceneCollectors {

	private final TopDocsCollector<?> topDocsCollector;

	private final TotalHitCountCollector totalHitCountCollector;

	private final Collector compositeCollector;

	LuceneCollectors(TopDocsCollector<?> topDocsCollector, TotalHitCountCollector totalHitCountCollector,
			Collector compositeCollector) {
		this.topDocsCollector = topDocsCollector;
		this.totalHitCountCollector = totalHitCountCollector;
		this.compositeCollector = compositeCollector;
	}

	long getTotalHits() {
		if ( topDocsCollector != null ) {
			return topDocsCollector.getTotalHits();
		}
		else {
			return totalHitCountCollector.getTotalHits();
		}
	}

	TopDocs getTopDocs(long firstResultIndex, Long maxResultsCount) {
		if ( topDocsCollector == null ) {
			return null;
		}

		if ( maxResultsCount == null ) {
			return topDocsCollector.topDocs( (int) firstResultIndex );
		}
		else {
			return topDocsCollector.topDocs( (int) firstResultIndex, maxResultsCount.intValue() );
		}
	}

	Collector getCompositeCollector() {
		return compositeCollector;
	}
}
