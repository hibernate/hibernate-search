/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.query.impl.LuceneChildrenCollector;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 * Regroups information about the data used as input of extraction (projections or aggregations):
 * collectors, stored fields, nested document IDs, ...
 */
public final class ExtractionRequirements {

	private final boolean requireTopDocs;
	private final boolean requireScore;
	private final Set<LuceneCollectorFactory<?>> requiredCollectorFactories;

	private final Set<String> requiredNestedDocumentExtractionPaths;

	private final Optional<ReusableDocumentStoredFieldVisitor> storedFieldVisitor;

	private ExtractionRequirements(Builder builder) {
		requireTopDocs = builder.requireTopDocs;
		requireScore = builder.requireScore;
		requiredCollectorFactories = builder.requiredCollectorFactories;
		requiredNestedDocumentExtractionPaths = builder.requiredNestedDocumentExtractionPaths;
		storedFieldVisitor = builder.createStoredFieldVisitor();
	}

	public LuceneCollectors createCollectors(Sort sort, int maxDocs, TimeoutManager timeoutManager) {
		TopDocsCollector<?> topDocsCollector = null;
		Integer scoreSortFieldIndexForRescoring = null;
		boolean requireFieldDocRescoring = false;

		Map<LuceneCollectorKey<?>, Collector> luceneCollectors = new LinkedHashMap<>();
		List<Collector> luceneCollectorsForNestedDocuments = new ArrayList<>();

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

		TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
		luceneCollectors.put( LuceneCollectorKey.TOTAL_HIT_COUNT, totalHitCountCollector );

		for ( LuceneCollectorFactory<?> collectorFactory : requiredCollectorFactories ) {
			Collector collector = collectorFactory.createCollector( maxDocs );
			luceneCollectors.put( collectorFactory, collector );
			if ( collectorFactory.applyToNestedDocuments() ) {
				luceneCollectorsForNestedDocuments.add( collector );
			}
		}

		LuceneChildrenCollector childrenCollector = null;
		if ( !requiredNestedDocumentExtractionPaths.isEmpty() ) {
			childrenCollector = new LuceneChildrenCollector();
			luceneCollectorsForNestedDocuments.add( childrenCollector );
		}

		Collector compositeCollector = wrapTimeLimitingCollectorIfNecessary(
				MultiCollector.wrap( luceneCollectors.values() ), timeoutManager
		);

		Collector compositeCollectorForNestedDocuments = null;
		if ( !luceneCollectorsForNestedDocuments.isEmpty() ) {
			compositeCollectorForNestedDocuments = wrapTimeLimitingCollectorIfNecessary(
					MultiCollector.wrap( luceneCollectorsForNestedDocuments ), timeoutManager
			);
		}

		return new LuceneCollectors(
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring,
				requiredNestedDocumentExtractionPaths,
				topDocsCollector, totalHitCountCollector, childrenCollector,
				compositeCollector, compositeCollectorForNestedDocuments,
				luceneCollectors,
				timeoutManager
		);
	}

	public Optional<ReusableDocumentStoredFieldVisitor> getStoredFieldVisitor() {
		return storedFieldVisitor;
	}

	private Collector wrapTimeLimitingCollectorIfNecessary(Collector collector, TimeoutManager timeoutManager) {
		final Long timeoutLeft = timeoutManager.checkTimeLeftInMilliseconds();
		if ( timeoutLeft != null ) {
			TimeLimitingCollector wrapped = new TimeLimitingCollector( collector, timeoutManager.createCounter(), timeoutLeft );
			// The timeout starts from now, not from when the collector is first used.
			// This is important because some collectors are applied during a second search.
			wrapped.setBaseline();
			return wrapped;
		}
		return collector;
	}

	public static class Builder {

		private boolean requireTopDocs;
		private boolean requireScore;
		private final Set<LuceneCollectorFactory<?>> requiredCollectorFactories = new LinkedHashSet<>();

		private final Set<String> requiredNestedDocumentExtractionPaths = new HashSet<>();

		private boolean requireAllStoredFields = false;
		private final Set<String> requiredStoredFields = new HashSet<>();

		public void requireScore() {
			this.requireTopDocs = true;
			this.requireScore = true;
		}

		public void requireTopDocsCollector() {
			this.requireTopDocs = true;
		}

		public <C extends Collector> void requireCollector(LuceneCollectorFactory<C> collectorFactory) {
			this.requireTopDocs = true; // We can't collect anything if we don't know from which documents it should be collected
			requiredCollectorFactories.add( collectorFactory );
		}

		public void requireNestedDocumentExtraction(String nestedDocumentPath) {
			if ( nestedDocumentPath != null ) {
				requireCollector( HibernateSearchDocumentIdToLuceneDocIdMapCollector.FACTORY );
				this.requiredNestedDocumentExtractionPaths.add( nestedDocumentPath );
			}
		}

		public void requireAllStoredFields() {
			requireAllStoredFields = true;
			requiredStoredFields.clear();
		}

		public void requireStoredField(String absoluteFieldPath) {
			if ( !requireAllStoredFields ) {
				requiredStoredFields.add( absoluteFieldPath );
			}
		}

		public ExtractionRequirements build() {
			return new ExtractionRequirements( this );
		}

		public Optional<ReusableDocumentStoredFieldVisitor> createStoredFieldVisitor() {
			if ( requireAllStoredFields ) {
				return Optional.of( new ReusableDocumentStoredFieldVisitor() );
			}
			else if ( !requiredStoredFields.isEmpty() ) {
				return Optional.of( new ReusableDocumentStoredFieldVisitor( requiredStoredFields ) );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
