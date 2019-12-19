/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.ChildrenCollector;
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
	private final Set<CollectorFactory<?>> requiredCollectorFactories;

	private final Set<String> requiredNestedDocumentExtractionPaths;

	private final Optional<ReusableDocumentStoredFieldVisitor> storedFieldVisitor;

	private ExtractionRequirements(Builder builder) {
		requireScore = builder.requireScore;
		requiredCollectorFactories = builder.requiredCollectorFactories;
		requiredNestedDocumentExtractionPaths = builder.requiredNestedDocumentExtractionPaths;
		storedFieldVisitor = builder.createStoredFieldVisitor();
	}

	public LuceneCollectors createCollectors(IndexSearcher indexSearcher, Query luceneQuery, Sort sort,
			IndexReaderMetadataResolver metadataResolver, int maxDocs, TimeoutManager timeoutManager)
			throws IOException {
		TopDocsCollector<?> topDocsCollector = null;
		Integer scoreSortFieldIndexForRescoring = null;
		boolean requireFieldDocRescoring = false;

		Map<CollectorKey<?>, Collector> luceneCollectors = new LinkedHashMap<>();

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
			luceneCollectors.put( CollectorKey.TOP_DOCS, topDocsCollector );
		}

		TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
		luceneCollectors.put( CollectorKey.TOTAL_HIT_COUNT, totalHitCountCollector );

		Map<String, NestedDocsProvider> nestedDocsProviders;
		ChildrenCollector childrenCollector = null;
		if ( requiredNestedDocumentExtractionPaths.isEmpty() ) {
			nestedDocsProviders = Collections.emptyMap();
		}
		else {
			nestedDocsProviders = new HashMap<>();
			for ( String nestedDocumentPath : requiredNestedDocumentExtractionPaths ) {
				nestedDocsProviders.put(
						nestedDocumentPath,
						new NestedDocsProvider( nestedDocumentPath, luceneQuery )
				);
			}

			NestedDocsProvider nestedDocsProvider =
					new NestedDocsProvider( requiredNestedDocumentExtractionPaths, luceneQuery );
			childrenCollector = new ChildrenCollector( indexSearcher, nestedDocsProvider );
			luceneCollectors.put( CollectorKey.CHILDREN, childrenCollector );
		}

		CollectorExecutionContext executionContext =
				new CollectorExecutionContext( metadataResolver, nestedDocsProviders, maxDocs );

		for ( CollectorFactory<?> collectorFactory : requiredCollectorFactories ) {
			Collector collector = collectorFactory.createCollector( executionContext );
			luceneCollectors.put( collectorFactory, collector );
		}

		Collector compositeCollector = wrapTimeLimitingCollectorIfNecessary(
				MultiCollector.wrap( luceneCollectors.values() ), timeoutManager
		);

		return new LuceneCollectors(
				indexSearcher,
				luceneQuery,
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring,
				topDocsCollector, totalHitCountCollector, childrenCollector,
				compositeCollector,
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

		private boolean requireScore;
		private final Set<CollectorFactory<?>> requiredCollectorFactories = new LinkedHashSet<>();

		private final Set<String> requiredNestedDocumentExtractionPaths = new HashSet<>();

		private boolean requireAllStoredFields = false;
		private final Set<String> requiredStoredFields = new HashSet<>();

		public void requireScore() {
			this.requireScore = true;
		}

		public <C extends Collector> void requireCollector(CollectorFactory<C> collectorFactory) {
			requiredCollectorFactories.add( collectorFactory );
		}

		public void requireNestedDocumentExtraction(String nestedDocumentPath) {
			if ( nestedDocumentPath != null ) {
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
