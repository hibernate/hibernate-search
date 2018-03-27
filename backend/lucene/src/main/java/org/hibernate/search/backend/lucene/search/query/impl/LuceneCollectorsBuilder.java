/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;

class LuceneCollectorsBuilder {

	private Sort sort;

	private int maxDocs;

	private TopDocsCollector<?> topDocsCollector;

	private TotalHitCountCollector totalHitCountCollector;

	private List<Collector> luceneCollectors = new ArrayList<>();

	LuceneCollectorsBuilder(Sort sort, int maxDocs) {
		this.sort = sort;
		this.maxDocs = maxDocs;
	}

	void requireTopDocsCollector() {
		if ( maxDocs == 0 ) {
			if ( totalHitCountCollector == null ) {
				totalHitCountCollector = new TotalHitCountCollector();
				luceneCollectors.add( totalHitCountCollector );
			}
		}
		else if ( topDocsCollector == null ) {
			topDocsCollector = createTopDocsCollector( sort, maxDocs );
			luceneCollectors.add( topDocsCollector );
		}
	}

	void addCollector(Collector collector) {
		luceneCollectors.add( collector );
	}

	LuceneCollectors build() {
		Collector compositeCollector;
		if ( luceneCollectors.size() == 1 ) {
			compositeCollector = topDocsCollector != null ? topDocsCollector : totalHitCountCollector;
		}
		else {
			compositeCollector = MultiCollector.wrap( luceneCollectors );
		}

		return new LuceneCollectors( topDocsCollector, totalHitCountCollector, compositeCollector );
	}

	private static TopDocsCollector<?> createTopDocsCollector(Sort sort, int maxDocs) {
		TopDocsCollector<?> topDocsCollector;
		if ( sort == null ) {
			topDocsCollector = TopScoreDocCollector.create( maxDocs );
		}
		else {
			topDocsCollector = TopFieldCollector.create(
					sort,
					maxDocs,
					true,
					true,
					true,
					true
			);
		}
		return topDocsCollector;
	}
}
