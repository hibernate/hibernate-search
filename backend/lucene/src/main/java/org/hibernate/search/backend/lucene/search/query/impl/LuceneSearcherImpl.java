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
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.util.common.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

class LuceneSearcherImpl<H> implements LuceneSearcher<LuceneLoadableSearchResult<H>> {

	private static final Log queryLog = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	private final LuceneSearchQueryRequestContext requestContext;

	private final LuceneSearchProjection<?, H> rootProjection;
	private final Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private final ExtractionRequirements extractionRequirements;

	private TimeoutManager timeoutManager;

	LuceneSearcherImpl(LuceneSearchQueryRequestContext requestContext,
			LuceneSearchProjection<?, H> rootProjection,
			Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations,
			ExtractionRequirements extractionRequirements,
			TimeoutManager timeoutManager) {
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
			int offset, Integer limit) throws IOException {
		queryLog.executingLuceneQuery( requestContext.getLuceneQuery() );

		LuceneCollectors luceneCollectors = buildCollectors( indexSearcher, metadataResolver, offset, limit );

		luceneCollectors.collect( offset, limit );

		LuceneSearchQueryExtractContext extractContext = requestContext.createExtractContext(
				indexSearcher, luceneCollectors
		);

		LuceneExtractableSearchResult<H> extractableSearchResult =
				new LuceneExtractableSearchResult<>( extractContext, rootProjection, aggregations, timeoutManager );
		return extractableSearchResult.extract();
	}

	@Override
	public int count(IndexSearcher indexSearcher) throws IOException {
		queryLog.executingLuceneQuery( requestContext.getLuceneQuery() );

		// Handling the hard timeout.
		// Soft timeout has no sense in case of count,
		// since there is no possible to have partial result.
		if ( timeoutManager.hasHardTimeout() ) {
			return indexSearcher.search( requestContext.getLuceneQuery(), new TimeoutCountCollectorManager( timeoutManager ) );
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
	public void setTimeoutManager(TimeoutManager timeoutManager) {
		this.timeoutManager = timeoutManager;
	}

	private LuceneCollectors buildCollectors(IndexSearcher indexSearcher, IndexReaderMetadataResolver metadataResolver,
			int offset, Integer limit) throws IOException {
		// TODO HSEARCH-3947 Check (and in case avoid) huge arrays are created for collectors when a query does not have an upper bound limit
		int maxDocs = getMaxDocs( indexSearcher.getIndexReader(), offset, limit );

		return extractionRequirements.createCollectors(
				indexSearcher, requestContext.getLuceneQuery(), requestContext.getLuceneSort(),
				metadataResolver, maxDocs, timeoutManager
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
