/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
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
	private final StoredFieldsValuesDelegate.Factory storedFieldsSourceFactoryOrNull;

	private ExtractionRequirements(Builder builder) {
		requireScore = builder.requireScore;
		requiredCollectorForAllMatchingDocsFactories = builder.requiredCollectorForAllMatchingDocsFactories;
		storedFieldsSourceFactoryOrNull = builder.createStoredFieldsSourceFactoryOrNull();
	}

	public LuceneCollectors createCollectors(IndexSearcher indexSearcher, Query originalLuceneQuery, Sort sort,
			IndexReaderMetadataResolver metadataResolver, int maxDocs, TimeoutManager timeoutManager,
			int requestedTotalHitCountThreshold)
			throws IOException {
		// Necessary to unwrap boolean queries with a single clause, in particular:
		// we have optimizations in place when there is a single query and this query is a MatchAllDocsQuery.
		Query rewrittenLuceneQuery = indexSearcher.rewrite( originalLuceneQuery );

		int totalHitCountThreshold;
		if ( rewrittenLuceneQuery instanceof MatchAllDocsQuery ) {
			// No need to keep track of the total hit count: we're matching everything,
			// so we can easily compute the total hit count in constant time.
			// See LuceneCollectors.collectMatchingDocs
			totalHitCountThreshold = 0;
		}
		else {
			totalHitCountThreshold = requestedTotalHitCountThreshold;
		}

		TopDocsCollector<?> topDocsCollector = null;
		Integer scoreSortFieldIndexForRescoring = null;
		boolean requireFieldDocRescoring = false;

		CollectorExecutionContext executionContext =
				new CollectorExecutionContext( metadataResolver, indexSearcher, maxDocs );

		CollectorSet.Builder collectorsForAllMatchingDocsBuilder =
				new CollectorSet.Builder( executionContext, timeoutManager );

		if ( maxDocs > 0 ) {
			if ( sort == null || isDescendingScoreSort( sort ) ) {
				topDocsCollector = TopScoreDocCollector.create( maxDocs, totalHitCountThreshold );
			}
			else {
				if ( requireScore ) {
					// Since https://issues.apache.org/jira/browse/LUCENE-8412 (Lucene 8.0.0),
					// TopFieldCollector returns TopDocs whose ScoreDocs do not contain a score...
					// Thus we will have to set the scores ourselves.
					requireFieldDocRescoring = true;
					// If there's a SCORE sort field, make sure we remember that, so that later we can optimize rescoring
					scoreSortFieldIndexForRescoring = getScoreSortFieldIndexOrNull( sort );
				}
				topDocsCollector = TopFieldCollector.create( sort, maxDocs, totalHitCountThreshold );
			}
			collectorsForAllMatchingDocsBuilder.add( LuceneCollectors.TOP_DOCS_KEY, topDocsCollector );
		}

		if ( topDocsCollector == null && totalHitCountThreshold > 0 ) {
			// Normally the topDocsCollector collects the total hit count,
			// but if it's not there and not all docs are matched, we need a separate collector.
			// Note that adding this collector can have a significant cost in some situations
			// (e.g. for queries matching many hits), so we only add it if it's really necessary.
			TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
			collectorsForAllMatchingDocsBuilder.add( LuceneCollectors.TOTAL_HIT_COUNT_KEY, totalHitCountCollector );
		}
		collectorsForAllMatchingDocsBuilder.addAll( requiredCollectorForAllMatchingDocsFactories );
		CollectorSet collectorsForAllMatchingDocs = collectorsForAllMatchingDocsBuilder.build();

		return new LuceneCollectors(
				metadataResolver,
				indexSearcher,
				rewrittenLuceneQuery,
				originalLuceneQuery,
				requireFieldDocRescoring, scoreSortFieldIndexForRescoring,
				collectorsForAllMatchingDocs,
				storedFieldsSourceFactoryOrNull,
				timeoutManager
		);
	}

	private boolean isDescendingScoreSort(Sort sort) {
		SortField[] fields = sort.getSort();
		return fields.length == 1 && isDescendingScoreSort( fields[0] );
	}

	private boolean isDescendingScoreSort(SortField sortField) {
		return SortField.Type.SCORE == sortField.getType() && !sortField.getReverse();
	}

	private Integer getScoreSortFieldIndexOrNull(Sort sort) {
		SortField[] sortFields = sort.getSort();
		for ( int i = 0; i < sortFields.length; i++ ) {
			SortField sortField = sortFields[i];
			if ( sortField.getType() == SortField.Type.SCORE ) {
				return i;
			}
		}
		return null;
	}

	public static class Builder {

		private boolean requireScore;
		private final Set<CollectorFactory<?>> requiredCollectorForAllMatchingDocsFactories = new LinkedHashSet<>();

		private boolean requireAllStoredFields = false;
		private final Set<String> requiredStoredFields = new HashSet<>();
		private final Set<String> requiredNestedDocumentPathsForStoredFields = new HashSet<>();

		public void requireScore() {
			this.requireScore = true;
		}

		public <C extends Collector> void requireCollectorForAllMatchingDocs(CollectorFactory<C> collectorFactory) {
			requiredCollectorForAllMatchingDocsFactories.add( collectorFactory );
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
			return new ExtractionRequirements( this );
		}

		private StoredFieldsValuesDelegate.Factory createStoredFieldsSourceFactoryOrNull() {
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

			return new StoredFieldsValuesDelegate.Factory( storedFieldVisitor, requiredNestedDocumentPathsForStoredFields );
		}
	}
}
