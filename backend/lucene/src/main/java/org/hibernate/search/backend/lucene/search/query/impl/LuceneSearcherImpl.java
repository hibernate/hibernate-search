/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectors;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.impl.LuceneNestedQueries;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.query.timout.impl.TimeoutCountCollectorManager;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.logging.impl.DefaultLogCategories;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;

class LuceneSearcherImpl<H> implements LuceneSearcher<LuceneLoadableSearchResult<H>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final Log queryLog = LoggerFactory.make( Log.class, DefaultLogCategories.QUERY );

	private static final Set<String> ID_FIELD_SET = Collections.singleton( LuceneFields.idFieldName() );

	private final LuceneSearchQueryRequestContext requestContext;

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final LuceneSearchProjection<?, H> rootProjection;
	private final Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private final TimingSource timingSource;

	private TimeoutManager timeoutManager;

	LuceneSearcherImpl(LuceneSearchQueryRequestContext requestContext,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			LuceneSearchProjection<?, H> rootProjection,
			Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations,
			TimeoutManager timeoutManager, TimingSource timingSource) {
		this.requestContext = requestContext;
		this.storedFieldVisitor = storedFieldVisitor;
		this.rootProjection = rootProjection;
		this.aggregations = aggregations;
		this.timeoutManager = timeoutManager;
		this.timingSource = timingSource;
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
	public LuceneLoadableSearchResult<H> search(IndexSearcher indexSearcher, int offset, Integer limit)
			throws IOException {
		queryLog.executingLuceneQuery( requestContext.getLuceneQuery() );

		LuceneCollectors luceneCollectors = buildCollectors( indexSearcher, offset, limit );

		executeQuery( indexSearcher, offset, limit, luceneCollectors );

		LuceneSearchQueryExtractContext extractContext = requestContext.createExtractContext(
				indexSearcher, luceneCollectors
		);

		List<Object> extractedData = extractHits( extractContext );

		Map<AggregationKey<?>, ?> extractedAggregations = aggregations.isEmpty() ?
				Collections.emptyMap() : extractAggregations( extractContext );

		return new LuceneLoadableSearchResult<>(
				extractContext, rootProjection,
				luceneCollectors.getTotalHits(),
				extractedData,
				extractedAggregations,
				timeoutManager.getTookTime(),
				timeoutManager.isTimedOut()
		);
	}

	@Override
	public int count(IndexSearcher indexSearcher) throws IOException {
		queryLog.executingLuceneQuery( requestContext.getLuceneQuery() );

		// Handling the hard timeout.
		// Soft timeout has no sense in case of count,
		// since there is no possible to have partial result.
		if ( TimeoutManager.Type.EXCEPTION.equals( timeoutManager.getType() ) ) {
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

	private LuceneCollectors buildCollectors(IndexSearcher indexSearcher, int offset, Integer limit) {
		// TODO HSEARCH-3323 this is very naive for now, we will probably need to implement some scrolling in the collector
		//  as it is done in Search 5.
		//  Note that Lucene initializes data structures of this size so setting it to a large value consumes memory.
		int maxDocs = getMaxDocs( indexSearcher.getIndexReader(), offset, limit );

		LuceneCollectorsBuilder luceneCollectorsBuilder =
				new LuceneCollectorsBuilder( requestContext.getLuceneSort(), maxDocs, timeoutManager, timingSource );
		rootProjection.contributeCollectors( luceneCollectorsBuilder );
		for ( LuceneSearchAggregation<?> aggregation : aggregations.values() ) {
			aggregation.contributeCollectors( luceneCollectorsBuilder );
		}
		return luceneCollectorsBuilder.build();
	}

	private void executeQuery(IndexSearcher indexSearcher, int offset, Integer limit, LuceneCollectors luceneCollectors) throws IOException {
		boolean timeOutBeforeQuery = timeoutManager.isTimedOut();
		if ( timeOutBeforeQuery ) {
			// in case of timeout before the query execution, skipping the query
			return;
		}

		try {
			luceneCollectors.collect( indexSearcher, requestContext.getLuceneQuery(), offset, limit );
		}
		catch (TimeLimitingCollector.TimeExceededException e) {
			timeoutManager.forceTimedOut();
		}
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

	private List<Object> extractHits(LuceneSearchQueryExtractContext extractContext) throws IOException {
		ProjectionHitMapper<?, ?> projectionHitMapper = extractContext.getProjectionHitMapper();
		IndexSearcher indexSearcher = extractContext.getIndexSearcher();

		TopDocs topDocs = extractContext.getTopDocs();
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		List<Object> extractedData = new ArrayList<>( topDocs.scoreDocs.length );
		Map<Integer, Set<Integer>> nestedDocs = fetchNestedDocs( indexSearcher, topDocs.scoreDocs, extractContext );

		SearchProjectionExtractContext projectionExtractContext = extractContext.createProjectionExtractContext( nestedDocs );

		for ( int i = 0; i < topDocs.scoreDocs.length; i++ ) {
			ScoreDoc hit = topDocs.scoreDocs[i];
			// add root object contribution
			indexSearcher.doc( hit.doc, storedFieldVisitor );
			if ( nestedDocs.containsKey( hit.doc ) ) {
				for ( Integer child : nestedDocs.get( hit.doc ) ) {
					indexSearcher.doc( child, storedFieldVisitor );
				}
			}

			Document document = storedFieldVisitor.getDocumentAndReset();
			LuceneResult luceneResult = new LuceneResult( document, hit.doc, hit.score );

			extractedData.add( rootProjection.extract( projectionHitMapper, luceneResult, projectionExtractContext ) );
			// Check for timeout each 16 elements:
			if ( i % 16 == 0 ) {
				timeoutManager.isTimedOut();
			}
		}

		return extractedData;
	}

	private Map<AggregationKey<?>, ?> extractAggregations(LuceneSearchQueryExtractContext extractContext)
			throws IOException {
		AggregationExtractContext aggregationExtractContext =
				extractContext.createAggregationExtractContext();

		Map<AggregationKey<?>, Object> extractedMap = new LinkedHashMap<>();

		for ( Map.Entry<AggregationKey<?>, LuceneSearchAggregation<?>> entry : aggregations.entrySet() ) {
			AggregationKey<?> key = entry.getKey();
			LuceneSearchAggregation<?> aggregation = entry.getValue();

			Object extracted = aggregation.extract( aggregationExtractContext );
			extractedMap.put( key, extracted );
		}

		return extractedMap;
	}

	private Map<Integer, Set<Integer>> fetchNestedDocs(IndexSearcher indexSearcher, ScoreDoc[] scoreDocs,
			LuceneSearchQueryExtractContext extractContext)
			throws IOException {
		// if the projection does not need any nested object skip their fetching
		if ( storedFieldVisitor.getNestedDocumentPaths().isEmpty() ) {
			return new HashMap<>();
		}

		// TODO HSEARCH-3657 this could be avoided
		Map<String, Integer> parentIds = new HashMap<>();
		for ( ScoreDoc hit : scoreDocs ) {
			Document doc = indexSearcher.doc( hit.doc, ID_FIELD_SET );
			String parentId = doc.getField( LuceneFields.idFieldName() ).stringValue();
			if ( parentId == null ) {
				continue;
			}
			parentIds.put( parentId, hit.doc );
		}

		Map<String, Set<Integer>> stringSetMap = fetchChildren(
				indexSearcher, storedFieldVisitor.getNestedDocumentPaths(),
				extractContext.getCollectorsForNestedDocuments()
		);
		HashMap<Integer, Set<Integer>> result = new HashMap<>();
		for ( Map.Entry<String, Set<Integer>> entry : stringSetMap.entrySet() ) {
			result.put( parentIds.get( entry.getKey() ), entry.getValue() );
		}
		return result;
	}

	private Map<String, Set<Integer>> fetchChildren(IndexSearcher indexSearcher, Set<String> nestedDocumentPaths,
			Collection<Collector> collectorsForChildren) {
		BooleanQuery booleanQuery = LuceneNestedQueries.findChildQuery( nestedDocumentPaths, requestContext.getLuceneQuery() );

		try {
			ArrayList<Collector> luceneCollectors = new ArrayList<>();
			LuceneChildrenCollector childrenCollector = new LuceneChildrenCollector();
			luceneCollectors.add( childrenCollector );
			luceneCollectors.addAll( collectorsForChildren );

			indexSearcher.search( booleanQuery, MultiCollector.wrap( luceneCollectors ) );
			return childrenCollector.getChildren();
		}
		catch (IOException e) {
			throw log.errorFetchingNestedDocuments( booleanQuery, e );
		}
	}
}
