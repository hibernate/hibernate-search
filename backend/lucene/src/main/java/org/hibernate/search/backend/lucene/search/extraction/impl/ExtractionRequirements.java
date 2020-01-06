/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsCollector;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
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

	private final boolean requireScore;
	private final Set<CollectorFactory<?>> requiredCollectorForAllMatchingDocsFactories;
	private final Set<CollectorFactory<?>> requiredCollectorForTopDocsFactories;

	private ExtractionRequirements(Builder builder) {
		requireScore = builder.requireScore;
		requiredCollectorForAllMatchingDocsFactories = builder.requiredCollectorForAllMatchingDocsFactories;
		requiredCollectorForTopDocsFactories = builder.requiredCollectorForTopDocsFactories;
	}

	public LuceneCollectors createCollectors(IndexSearcher indexSearcher, Query luceneQuery, Sort sort,
			IndexReaderMetadataResolver metadataResolver, int maxDocs, TimeoutManager timeoutManager)
			throws IOException {
		TopDocsCollector<?> topDocsCollector = null;
		Integer scoreSortFieldIndexForRescoring = null;
		boolean requireFieldDocRescoring = false;

		Map<CollectorKey<?>, Collector> collectorForAllMatchingDocsMap = new LinkedHashMap<>();
		Map<CollectorKey<?>, Collector> collectorForTopDocsMap = new LinkedHashMap<>();

		if ( maxDocs > 0 ) {
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
			collectorForAllMatchingDocsMap.put( CollectorKey.TOP_DOCS, topDocsCollector );
		}

		TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
		collectorForAllMatchingDocsMap.put( CollectorKey.TOTAL_HIT_COUNT, totalHitCountCollector );

		CollectorExecutionContext executionContext =
				new CollectorExecutionContext( metadataResolver, indexSearcher, luceneQuery, maxDocs );

		for ( CollectorFactory<?> collectorFactory : requiredCollectorForAllMatchingDocsFactories ) {
			Collector collector = collectorFactory.createCollector( executionContext );
			collectorForAllMatchingDocsMap.put( collectorFactory.getCollectorKey(), collector );
		}
		CollectorSet collectorsForAllMatchingDocs = new CollectorSet(
				wrapTimeLimitingCollectorIfNecessary(
						MultiCollector.wrap( collectorForAllMatchingDocsMap.values() ), timeoutManager
				),
				collectorForAllMatchingDocsMap
		);

		for ( CollectorFactory<?> collectorFactory : requiredCollectorForTopDocsFactories ) {
			Collector collector = collectorFactory.createCollector( executionContext );
			collectorForTopDocsMap.put( collectorFactory.getCollectorKey(), collector );
		}
		CollectorSet collectorsForTopDocs = null;
		if ( !collectorForTopDocsMap.isEmpty() ) {
			collectorsForTopDocs = new CollectorSet(
					wrapTimeLimitingCollectorIfNecessary(
							MultiCollector.wrap( collectorForTopDocsMap.values() ), timeoutManager
					),
					collectorForTopDocsMap
			);
		}

		return new LuceneCollectors(
				indexSearcher,
				luceneQuery,
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring,
				collectorsForAllMatchingDocs, collectorsForTopDocs,
				timeoutManager
		);
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

		private boolean requireScore;
		private final Set<CollectorFactory<?>> requiredCollectorForAllMatchingDocsFactories = new LinkedHashSet<>();
		private final Set<CollectorFactory<?>> requiredCollectorForTopDocsFactories = new LinkedHashSet<>();

		private boolean requireAllStoredFields = false;
		private final Set<String> requiredStoredFields = new HashSet<>();
		private final Set<String> requiredNestedDocumentPathsForStoredFields = new HashSet<>();

		public void requireScore() {
			this.requireScore = true;
		}

		public <C extends Collector> void requireCollectorForAllMatchingDocs(CollectorFactory<C> collectorFactory) {
			requiredCollectorForAllMatchingDocsFactories.add( collectorFactory );
		}

		public <C extends Collector> void requireCollectorForTopDocs(CollectorFactory<C> collectorFactory) {
			requiredCollectorForTopDocsFactories.add( collectorFactory );
		}

		public void requireAllStoredFields() {
			requireAllStoredFields = true;
			requiredStoredFields.clear();
		}

		public void requireStoredField(String absoluteFieldPath, String nestedDocumentPath) {
			if ( !requireAllStoredFields ) {
				requiredStoredFields.add( absoluteFieldPath );
			}
			if ( nestedDocumentPath != null ) {
				requiredNestedDocumentPathsForStoredFields.add( nestedDocumentPath );
			}
		}

		public ExtractionRequirements build() {
			CollectorFactory<StoredFieldsCollector> storedFieldCollectorFactory = createStoredFieldCollectorFactoryOrNull();
			if ( storedFieldCollectorFactory != null ) {
				requiredCollectorForTopDocsFactories.add( storedFieldCollectorFactory );
			}
			return new ExtractionRequirements( this );
		}

		private CollectorFactory<StoredFieldsCollector> createStoredFieldCollectorFactoryOrNull() {
			ReusableDocumentStoredFieldVisitor storedFieldVisitor;
			if ( requireAllStoredFields ) {
				storedFieldVisitor = new ReusableDocumentStoredFieldVisitor();
			}
			else if ( !requiredStoredFields.isEmpty() ) {
				storedFieldVisitor = new ReusableDocumentStoredFieldVisitor( requiredStoredFields );
			}
			else {
				return null;
			}

			return StoredFieldsCollector.factory( storedFieldVisitor, requiredNestedDocumentPathsForStoredFields );
		}
	}
}
