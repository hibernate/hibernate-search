/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.types.sort.nested.impl.LuceneNestedDocumentsSort;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;

public class LuceneCollectorsBuilder {

	private final Sort sort;
	private final LuceneNestedDocumentsSort nestedDocumentsSort;
	private final int maxDocs;

	private final TotalHitCountCollector totalHitCountCollector;

	private boolean requireTopDocs;
	private boolean requireScore;

	private final Map<LuceneCollectorKey<?>, Collector> luceneCollectors = new LinkedHashMap<>();
	private final List<Collector> luceneCollectorsForNestedDocuments = new ArrayList<>();

	public LuceneCollectorsBuilder(Sort sort, LuceneNestedDocumentsSort nestedDocumentsSort, int maxDocs) {
		this.sort = sort;
		this.nestedDocumentsSort = nestedDocumentsSort;
		this.maxDocs = maxDocs;

		this.totalHitCountCollector = new TotalHitCountCollector();
		this.luceneCollectors.put( LuceneCollectorKey.TOTAL_HIT_COUNT, this.totalHitCountCollector );
	}

	public void requireScore() {
		this.requireTopDocs = true;
		this.requireScore = true;
	}

	public void requireTopDocsCollector() {
		this.requireTopDocs = true;
	}

	public <C extends Collector> void addCollector(LuceneCollectorFactory<C> collectorFactory) {
		this.requireTopDocs = true; // We can't collect anything if we don't know from which documents it should be collected
		if ( luceneCollectors.containsKey( collectorFactory ) ) {
			return;
		}

		Collector collector = collectorFactory.createCollector( maxDocs );
		luceneCollectors.put( collectorFactory, collector );
		if ( collectorFactory.applyToNestedDocuments() ) {
			luceneCollectorsForNestedDocuments.add( collector );
		}
	}

	public LuceneCollectors build() {
		TopDocsCollector<?> topDocsCollector = null;
		Integer scoreSortFieldIndexForRescoring = null;
		boolean requireFieldDocRescoring = false;

		if ( requireTopDocs && maxDocs > 0 ) {
			if ( sort == null ) {
				topDocsCollector = TopScoreDocCollector.create(
						maxDocs,
						// TODO HSEARCH-3517 Avoid tracking the total hit count when possible
						// Note this will also require to change how we combine collectors,
						// as MultiCollector explicitly ignores the total hit count optimization
						Integer.MAX_VALUE
				);
			}
			else {
				if ( requireScore ) {
					// Since https://issues.apache.org/jira/browse/LUCENE-8412 (Lucene 8.0.0),
					// TopFieldCollector returns TopDocs whose ScoreDocs do not contain a score...
					// Thus we will have to set the scores ourselves.
					requireFieldDocRescoring = true;

					// If there's a SCORE sort field, make sure we remember that, so that later we can optimize rescoring
					SortField[] sortFields = sort.getSort();
					for ( int i = 0; i < sortFields.length; i++ ) {
						SortField sortField = sortFields[i];
						if ( sortField.getType() == SortField.Type.SCORE ) {
							scoreSortFieldIndexForRescoring = i;
							break;
						}
					}
				}
				topDocsCollector = TopFieldCollector.create(
						sort,
						maxDocs,
						// TODO HSEARCH-3517 Avoid tracking the total hit count when possible
						// Note this will also require to change how we combine collectors,
						// as MultiCollector explicitly ignores the total hit count optimization
						Integer.MAX_VALUE
				);
			}
			luceneCollectors.put( LuceneCollectorKey.TOP_DOCS, topDocsCollector );
		}

		Collector compositeCollector = MultiCollector.wrap( luceneCollectors.values() );

		return new LuceneCollectors(
				topDocsCollector, totalHitCountCollector,
				compositeCollector, luceneCollectorsForNestedDocuments,
				luceneCollectors,
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring,
				sort, nestedDocumentsSort, maxDocs
		);
	}
}
