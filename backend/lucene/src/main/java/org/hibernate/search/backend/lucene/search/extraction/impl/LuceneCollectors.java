/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.ExplicitDocIdsQuery;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.TotalHits;

public class LuceneCollectors {

	static final CollectorKey<TotalHitCountCollector> TOTAL_HIT_COUNT_KEY = CollectorKey.create();
	static final CollectorKey<TopDocsCollector<?>> TOP_DOCS_KEY = CollectorKey.create();

	private final IndexReaderMetadataResolver metadataResolver;

	private final IndexSearcher indexSearcher;
	private final Query rewrittenLuceneQuery;

	private final boolean requireFieldDocRescoring;
	private final Integer scoreSortFieldIndexForRescoring;

	private final CollectorSet collectorsForAllMatchingDocs;
	private final Set<CollectorFactory<?>> collectorsForTopDocsFactories;
	private CollectorSet collectorsForTopDocs;

	private final TimeoutManager timeoutManager;

	private SearchResultTotal resultTotal;
	private TopDocs topDocs = null;

	LuceneCollectors(IndexReaderMetadataResolver metadataResolver, IndexSearcher indexSearcher, Query rewrittenLuceneQuery,
			boolean requireFieldDocRescoring, Integer scoreSortFieldIndexForRescoring,
			CollectorSet collectorsForAllMatchingDocs,
			Set<CollectorFactory<?>> collectorsForTopDocsFactories,
			TimeoutManager timeoutManager) {
		this.metadataResolver = metadataResolver;
		this.indexSearcher = indexSearcher;
		this.rewrittenLuceneQuery = rewrittenLuceneQuery;
		this.requireFieldDocRescoring = requireFieldDocRescoring;
		this.scoreSortFieldIndexForRescoring = scoreSortFieldIndexForRescoring;
		this.collectorsForAllMatchingDocs = collectorsForAllMatchingDocs;
		this.collectorsForTopDocsFactories = collectorsForTopDocsFactories;
		this.timeoutManager = timeoutManager;
	}

	/**
	 * Phase 1: collect matching docs.
	 * Collects the total hit count, aggregations, and top docs.
	 *
	 * @param offset The index of the first collected top doc.
	 * @param limit The maximum amount of top docs to collect.
	 * @throws IOException If Lucene throws an {@link IOException}.
	 */
	public void collectMatchingDocs(int offset, Integer limit) throws IOException {
		if ( timeoutManager.checkTimedOut() ) {
			resultTotal = SimpleSearchResultTotal.lowerBound( 0L );
			// in case of timeout before the query execution, skip the query
			return;
		}

		try {
			Collector composed = collectorsForAllMatchingDocs.getComposed();
			if ( composed != null ) {
				indexSearcher.search( rewrittenLuceneQuery, composed );
			}
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			Deadline deadline = timeoutManager.deadlineOrNull();
			if ( deadline == null ) {
				throw new AssertionFailure( "Timeout reached, but no timeout was defined", e );
			}
			deadline.forceTimeout( e );
		}

		processCollectedMatchingDocs( offset, limit );
	}

	private void processCollectedMatchingDocs(int offset, Integer limit) throws IOException {
		if ( rewrittenLuceneQuery instanceof MatchAllDocsQuery ) {
			// We can compute the total hit count in constant time.
			resultTotal = SimpleSearchResultTotal.exact( indexSearcher.getIndexReader().numDocs() );
		}
		else {
			TotalHitCountCollector totalHitCountCollector = collectorsForAllMatchingDocs.get( TOTAL_HIT_COUNT_KEY );
			if ( totalHitCountCollector != null ) {
				boolean exact = !timeoutManager.isTimedOut();
				resultTotal = SimpleSearchResultTotal.of( totalHitCountCollector.getTotalHits(), exact );
			}
		}

		TopDocsCollector<?> topDocsCollector = collectorsForAllMatchingDocs.get( TOP_DOCS_KEY );
		if ( topDocsCollector == null ) {
			if ( resultTotal == null ) {
				resultTotal = SimpleSearchResultTotal.lowerBound( 0 );
			}
			return;
		}

		extractTopDocs( topDocsCollector, offset, limit );
		if ( resultTotal == null ) {
			boolean exact = TotalHits.Relation.EQUAL_TO.equals( topDocs.totalHits.relation )
					&& !timeoutManager.isTimedOut();
			resultTotal = SimpleSearchResultTotal.of( topDocs.totalHits.value, exact );
		}
		else if ( resultTotal.isHitCountExact() ) {
			// Update the total hit count of the topDocs, which might not be precise enough,
			// and might be consumed by callers of LuceneSearchResult.topDocs()
			// (Useful for Infinispan in particular)
			topDocs.totalHits = new TotalHits( resultTotal.hitCount(), TotalHits.Relation.EQUAL_TO );
		}

		if ( requireFieldDocRescoring ) {
			handleRescoring();
		}
	}

	public CollectorSet getCollectorsForAllMatchingDocs() {
		return collectorsForAllMatchingDocs;
	}

	/**
	 * Phase 2: collect data relative to top docs.
	 *
	 * @param startInclusive The index of the first top doc whose data to collect.
	 * @param endExclusive The index after the last top doc whose data to collect.
	 *
	 * @throws IOException If Lucene throws an {@link IOException}.
	 */
	public void collectTopDocsData(int startInclusive, int endExclusive) throws IOException {
		if ( collectorsForTopDocsFactories.isEmpty() || topDocs == null ) {
			this.collectorsForTopDocs = null;
			return;
		}
		try {
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			ExplicitDocIdsQuery topDocsQuery = new ExplicitDocIdsQuery( scoreDocs, startInclusive, endExclusive );
			this.collectorsForTopDocs = buildTopdDocsDataCollectors();
			indexSearcher.search( topDocsQuery, collectorsForTopDocs.getComposed() );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			Deadline deadline = timeoutManager.deadlineOrNull();
			if ( deadline == null ) {
				throw new AssertionFailure( "Timeout reached, but no timeout was defined", e );
			}
			deadline.forceTimeout( e );
		}
	}

	public CollectorSet getCollectorsForTopDocs() {
		return collectorsForTopDocs;
	}

	public SearchResultTotal getResultTotal() {
		return resultTotal;
	}

	public TopDocs getTopDocs() {
		return topDocs;
	}

	private void extractTopDocs(TopDocsCollector<?> topDocsCollector, int offset, Integer limit) {
		if ( offset >= topDocsCollector.getTotalHits() ) {
			// Hack.
			// In this case, we cannot execute the code below as Lucene considers we passed incorrect arguments
			// and returns a TopDocs instance with no ScoreDocs (correct)
			// and a total hit count set to 0 (not always correct).
			// Also, we cannot reconstruct the total hit count from just the collector
			// since we don't have access to the relation (EQUAL/GT_OR_EQUAL).
			// So we get just one topDoc, and infer everything from there.
			TopDocs firstTopDoc = topDocsCollector.topDocs( 0, 1 );
			topDocs = firstTopDoc instanceof TopFieldDocs
					? new TopFieldDocs( firstTopDoc.totalHits, new FieldDoc[0], ( (TopFieldDocs) firstTopDoc ).fields )
					: new TopDocs( firstTopDoc.totalHits, new ScoreDoc[0] );
			return;
		}

		if ( limit == null ) {
			topDocs = topDocsCollector.topDocs( offset );
		}
		else {
			topDocs = topDocsCollector.topDocs( offset, limit );
		}
	}

	private void handleRescoring() throws IOException {
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
			TopFieldCollector.populateScores( topDocs.scoreDocs, indexSearcher, rewrittenLuceneQuery );
		}
	}

	private CollectorSet buildTopdDocsDataCollectors() throws IOException {
		CollectorExecutionContext executionContext = new CollectorExecutionContext(
				metadataResolver, indexSearcher,
				// Allocate just enough memory to handle the top documents.
				topDocs.scoreDocs.length
		);

		CollectorSet.Builder collectorForTopDocsBuilder =
				new CollectorSet.Builder( executionContext, timeoutManager );
		collectorForTopDocsBuilder.addAll( collectorsForTopDocsFactories );
		return collectorForTopDocsBuilder.build();
	}
}
