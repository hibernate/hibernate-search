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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.extraction.impl.DistanceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearchResultExtractor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ToChildBlockJoinQuery;

class LuceneSearchResultExtractorImpl<H> implements LuceneSearchResultExtractor<LuceneLoadableSearchResult<H>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Set<String> ID_FIELD_SET = Collections.singleton( LuceneFields.idFieldName() );

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final LuceneSearchProjection<?, H> rootProjection;
	private final LoadingContext<?, ?> loadingContext;

	LuceneSearchResultExtractorImpl(
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			LuceneSearchProjection<?, H> rootProjection,
			LoadingContext<?, ?> loadingContext) {
		this.storedFieldVisitor = storedFieldVisitor;
		this.rootProjection = rootProjection;
		this.loadingContext = loadingContext;
	}

	@Override
	public LuceneLoadableSearchResult<H> extract(IndexSearcher indexSearcher, long totalHits, TopDocs topDocs,
			SearchProjectionExtractContext projectionExecutionContext) throws IOException {
		ProjectionHitMapper<?, ?> projectionHitMapper = loadingContext.getProjectionHitMapper();

		List<Object> extractedData = extractHits(
				projectionHitMapper, indexSearcher, topDocs, projectionExecutionContext
		);

		return new LuceneLoadableSearchResult<>(
				projectionHitMapper, rootProjection,
				totalHits, extractedData
		);
	}

	private List<Object> extractHits(ProjectionHitMapper<?, ?> projectionHitMapper,
			IndexSearcher indexSearcher, TopDocs topDocs,
			SearchProjectionExtractContext projectionExecutionContext) throws IOException {
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		List<Object> extractedData = new ArrayList<>( topDocs.scoreDocs.length );
		Map<Integer, Set<Integer>> nestedDocs = fetchNestedDocs( indexSearcher, topDocs.scoreDocs, projectionExecutionContext );

		for ( ScoreDoc hit : topDocs.scoreDocs ) {
			// add root object contribution
			indexSearcher.doc( hit.doc, storedFieldVisitor );
			if ( nestedDocs.containsKey( hit.doc ) ) {
				for ( Integer child : nestedDocs.get( hit.doc ) ) {
					indexSearcher.doc( child, storedFieldVisitor );
				}
			}

			Document document = storedFieldVisitor.getDocumentAndReset();
			LuceneResult luceneResult = new LuceneResult( document, hit.doc, hit.score );

			extractedData.add( rootProjection.extract( projectionHitMapper, luceneResult, projectionExecutionContext ) );
		}

		return extractedData;
	}

	private Map<Integer, Set<Integer>> fetchNestedDocs(IndexSearcher indexSearcher, ScoreDoc[] scoreDocs, SearchProjectionExtractContext projectionExecutionContext)
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

		Map<String, Set<Integer>> stringSetMap = fetchChildren( indexSearcher, storedFieldVisitor.getNestedDocumentPaths(), projectionExecutionContext );
		HashMap<Integer, Set<Integer>> result = new HashMap<>();
		for ( Map.Entry<String, Set<Integer>> entry : stringSetMap.entrySet() ) {
			result.put( parentIds.get( entry.getKey() ), entry.getValue() );
		}
		for ( DistanceCollector distanceCollector : projectionExecutionContext.getDistanceCollectors() ) {
			distanceCollector.setNestedDocs( result );
		}
		return result;
	}

	private Map<String, Set<Integer>> fetchChildren(IndexSearcher indexSearcher, Set<String> nestedDocumentPaths,
			SearchProjectionExtractContext projectionExecutionContext) {
		BooleanQuery booleanQuery = getChildQuery( nestedDocumentPaths, projectionExecutionContext );

		try {
			ArrayList<Collector> luceneCollectors = new ArrayList<>();
			LuceneChildrenCollector childrenCollector = new LuceneChildrenCollector();
			luceneCollectors.add( childrenCollector );
			luceneCollectors.addAll( projectionExecutionContext.getDistanceCollectors() );

			indexSearcher.search( booleanQuery, MultiCollector.wrap( luceneCollectors ) );
			return childrenCollector.getChildren();
		}
		catch (IOException e) {
			throw log.errorFetchingNestedDocuments( booleanQuery, e );
		}
	}

	private BooleanQuery getChildQuery(Set<String> nestedDocumentPaths, SearchProjectionExtractContext projectionExecutionContext) {
		QueryBitSetProducer parentsFilter = new QueryBitSetProducer( LuceneQueries.mainDocumentQuery() );
		ToChildBlockJoinQuery parentQuery = new ToChildBlockJoinQuery( projectionExecutionContext.getLuceneQuery(), parentsFilter );

		return new BooleanQuery.Builder()
				.add( parentQuery, BooleanClause.Occur.MUST )
				.add( createNestedDocumentPathSubQuery( nestedDocumentPaths ), BooleanClause.Occur.FILTER )
				.add( LuceneQueries.childDocumentQuery(), BooleanClause.Occur.FILTER )
				.build();
	}

	private BooleanQuery createNestedDocumentPathSubQuery(Set<String> nestedDocumentPaths) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( String nestedDocumentPath : nestedDocumentPaths ) {
			builder.add( LuceneQueries.nestedDocumentPathQuery( nestedDocumentPath ), BooleanClause.Occur.SHOULD );
		}
		builder.setMinimumNumberShouldMatch( 1 );
		return builder.build();
	}
}
