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
import java.util.Set;

import org.hibernate.search.backend.lucene.search.query.impl.LuceneChildrenCollector;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.backend.lucene.search.timeout.impl.LuceneCounterAdapter;
import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.util.Counter;

public class LuceneCollectorsBuilder {

	private final Sort sort;
	private final Set<String> nestedDocumentPaths;
	private final int maxDocs;
	private final TimeoutManager timeoutManager;
	private final TimingSource timingSource;

	private final TotalHitCountCollector totalHitCountCollector;

	private boolean requireTopDocs;
	private boolean requireScore;

	private final Map<LuceneCollectorKey<?>, Collector> luceneCollectors = new LinkedHashMap<>();
	private final List<Collector> luceneCollectorsForNestedDocuments = new ArrayList<>();

	public LuceneCollectorsBuilder(Sort sort, Set<String> nestedDocumentPaths,
			int maxDocs, TimeoutManager timeoutManager, TimingSource timingSource) {
		this.sort = sort;
		this.nestedDocumentPaths = nestedDocumentPaths;
		this.maxDocs = maxDocs;
		this.timeoutManager = timeoutManager;
		this.timingSource = timingSource;

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

		Collector compositeCollector = wrapTimeLimitingCollectorIfNecessary(
				MultiCollector.wrap( luceneCollectors.values() )
		);

		LuceneChildrenCollector childrenCollector = null;
		if ( !nestedDocumentPaths.isEmpty() ) {
			childrenCollector = new LuceneChildrenCollector();
			luceneCollectorsForNestedDocuments.add( childrenCollector );
		}
		Collector compositeCollectorForNestedDocuments = null;
		if ( !luceneCollectorsForNestedDocuments.isEmpty() ) {
			compositeCollectorForNestedDocuments = wrapTimeLimitingCollectorIfNecessary(
					MultiCollector.wrap( luceneCollectorsForNestedDocuments )
			);
		}

		return new LuceneCollectors(
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring,
				nestedDocumentPaths,
				topDocsCollector, totalHitCountCollector, childrenCollector,
				compositeCollector, compositeCollectorForNestedDocuments,
				luceneCollectors,
				timeoutManager
		);
	}

	private Collector wrapTimeLimitingCollectorIfNecessary(Collector collector) {
		final Long timeoutLeft = timeoutManager.checkTimeLeftInMilliseconds();
		if ( timeoutLeft != null ) {
			Counter counter = new LuceneCounterAdapter( timingSource );
			TimeLimitingCollector wrapped = new TimeLimitingCollector( collector, counter, timeoutLeft );
			// The timeout starts from now, not from when the collector is first used.
			// This is important because some collectors are applied during a second search.
			wrapped.setBaseline();
			return wrapped;
		}
		return collector;
	}
}
