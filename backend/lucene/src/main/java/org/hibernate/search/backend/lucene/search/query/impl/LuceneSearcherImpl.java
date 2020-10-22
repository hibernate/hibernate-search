/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TimeoutCountCollectorManager;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.extraction.impl.ExtractionRequirements;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectors;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.timeout.impl.LuceneTimeoutManager;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.util.common.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

class LuceneSearcherImpl<H> implements LuceneSearcher<LuceneLoadableSearchResult<H>, LuceneExtractableSearchResult<H>> {

	private static final int PREFETCH_HITS_SIZE = 100;
	private static final int PREFETCH_TOTAL_HIT_COUNT_THRESHOLD = 10_000;

	private static final Log queryLog = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	private final LuceneSearchQueryRequestContext requestContext;

	private final LuceneSearchProjection<?, H> rootProjection;
	private final Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private final ExtractionRequirements extractionRequirements;

	private LuceneTimeoutManager timeoutManager;

	LuceneSearcherImpl(LuceneSearchQueryRequestContext requestContext,
			LuceneSearchProjection<?, H> rootProjection,
			Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations,
			ExtractionRequirements extractionRequirements,
			LuceneTimeoutManager timeoutManager) {
		this.requestContext = requestContext;
		this.rootProjection = rootProjection;
		this.aggregations = aggregations;
		this.extractionRequirements = extractionRequirements;
		this.timeoutManager = timeoutManager;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "luceneQuery=" ).append( requestContext.getLuceneQuery() )
				.append( ", luceneSort=" ).append( requestContext.getLuceneSort() )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public LuceneLoadableSearchResult<H> search(IndexSearcher indexSearcher,
			IndexReaderMetadataResolver metadataResolver,
			int offset, Integer limit, int totalHitCountThreshold) throws IOException {
		return doSearch( indexSearcher, metadataResolver, offset, limit, totalHitCountThreshold ).extract();
	}

	@Override
	public LuceneExtractableSearchResult<H> scroll(IndexSearcher indexSearcher,
			IndexReaderMetadataResolver metadataResolver, int offset, int limit) throws IOException {
		return doSearch( indexSearcher, metadataResolver, offset, limit, Integer.MAX_VALUE );
	}

	private LuceneExtractableSearchResult<H> doSearch(IndexSearcher indexSearcher,
			IndexReaderMetadataResolver metadataResolver,
			int offset, Integer limit, int totalHitCountThreshold) throws IOException {
		queryLog.executingLuceneQuery( requestContext.getLuceneQuery() );

		int maxDocs = getMaxDocs( indexSearcher.getIndexReader(), offset, limit );
		LuceneCollectors luceneCollectors = ( limit != null || maxDocs <= PREFETCH_HITS_SIZE ) ?
				collectMatchingDocs( indexSearcher, metadataResolver, offset, limit, maxDocs, totalHitCountThreshold ) :
				collectMatchingDocsWithPrefetch( indexSearcher, metadataResolver, offset, limit, maxDocs, totalHitCountThreshold );

		return new LuceneExtractableSearchResult<>( requestContext, indexSearcher, luceneCollectors,
				rootProjection, aggregations, timeoutManager );
	}

	@Override
	public int count(IndexSearcher indexSearcher) throws IOException {
		queryLog.executingLuceneQuery( requestContext.getLuceneQuery() );

		// Handling the hard timeout.
		// Soft timeout has no sense in case of count,
		// since there is no possible to have partial result.
		if ( timeoutManager.hasHardTimeout() ) {
			return indexSearcher.search(
					requestContext.getLuceneQuery(), new TimeoutCountCollectorManager( timeoutManager ) );
		}

		return indexSearcher.count( requestContext.getLuceneQuery() );
	}

	@Override
	public Explanation explain(IndexSearcher indexSearcher, int luceneDocId) throws IOException {
		return indexSearcher.explain( requestContext.getLuceneQuery(), luceneDocId );
	}

	@Override
	public Query getLuceneQueryForExceptions() {
		return requestContext.getLuceneQuery();
	}

	@Override
	public void setTimeoutManager(LuceneTimeoutManager timeoutManager) {
		this.timeoutManager = timeoutManager;
	}

	private LuceneCollectors collectMatchingDocs(IndexSearcher indexSearcher,
			IndexReaderMetadataResolver metadataResolver, int offset, Integer limit,
			int maxDocs, int totalHitCountThreshold) throws IOException {
		LuceneCollectors luceneCollectors = buildCollectors( indexSearcher, metadataResolver,
				maxDocs, totalHitCountThreshold );
		luceneCollectors.collectMatchingDocs( offset, limit );
		return luceneCollectors;
	}

	private LuceneCollectors collectMatchingDocsWithPrefetch(IndexSearcher indexSearcher,
			IndexReaderMetadataResolver metadataResolver, int offset, Integer limit,
			int maxDocs, int totalHitCountThreshold) throws IOException {

		// prefetch:
		LuceneCollectors luceneCollectors = collectMatchingDocs( indexSearcher, metadataResolver, offset, limit,
				PREFETCH_HITS_SIZE, Math.max( totalHitCountThreshold, PREFETCH_TOTAL_HIT_COUNT_THRESHOLD ) );

		SearchResultTotal resultTotal = luceneCollectors.getResultTotal();
		if ( resultTotal.isHitCountLowerBound() || resultTotal.hitCount() > PREFETCH_TOTAL_HIT_COUNT_THRESHOLD ) {
			// if the total hit count is unbounded, we need to execute the unbounded query
			return collectMatchingDocs( indexSearcher, metadataResolver, offset, limit, maxDocs, maxDocs );
		}

		if ( resultTotal.hitCount() < PREFETCH_HITS_SIZE ) {
			// if the total hit count is less than the prefetch, we don't need to execute any further query
			return luceneCollectors;
		}

		// if the total hit count is in the middle between the two cases above, we can execute a bounded query
		int exactHitCount = Math.toIntExact( resultTotal.hitCount() );
		return collectMatchingDocs( indexSearcher, metadataResolver, offset, limit, exactHitCount, exactHitCount );
	}

	private LuceneCollectors buildCollectors(IndexSearcher indexSearcher, IndexReaderMetadataResolver metadataResolver,
			int maxDocs, int totalHitCountThreshold) throws IOException {
		return extractionRequirements.createCollectors(
				indexSearcher, requestContext.getLuceneQuery(), requestContext.getLuceneSort(),
				metadataResolver, maxDocs, timeoutManager, totalHitCountThreshold
		);
	}

	private int getMaxDocs(IndexReader reader, int offset, Integer limit) {
		if ( limit == null ) {
			return reader.maxDoc();
		}
		else if ( limit.equals( 0 ) ) {
			return 0;
		}
		else {
			return Math.min( offset + limit, reader.maxDoc() );
		}
	}
}
