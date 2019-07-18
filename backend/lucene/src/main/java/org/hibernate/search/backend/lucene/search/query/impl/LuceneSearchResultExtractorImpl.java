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
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

class LuceneSearchResultExtractorImpl<H> implements LuceneSearchResultExtractor<H> {

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
		Map<Integer, Set<Integer>> nestedDocs = fetchNestedDocs( indexSearcher, topDocs.scoreDocs );

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

	private Map<Integer, Set<Integer>> fetchNestedDocs(IndexSearcher indexSearcher, ScoreDoc[] scoreDocs) throws IOException {
		// if the projection does not need any nested object skip their fetching
		if ( storedFieldVisitor.getNestedDocumentPaths().isEmpty() ) {
			return new HashMap<>();
		}

		Map<String, Integer> parentIds = new HashMap<>();
		for ( ScoreDoc hit : scoreDocs ) {
			Document doc = indexSearcher.doc( hit.doc, ID_FIELD_SET );
			String parentId = doc.getField( LuceneFields.idFieldName() ).stringValue();
			if ( parentId == null ) {
				continue;
			}
			parentIds.put( parentId, hit.doc );
		}

		Map<String, Set<Integer>> stringSetMap = fetchChildren( indexSearcher, parentIds.keySet(), storedFieldVisitor.getNestedDocumentPaths() );
		HashMap<Integer, Set<Integer>> result = new HashMap<>();
		for ( Map.Entry<String, Set<Integer>> entry : stringSetMap.entrySet() ) {
			result.put( parentIds.get( entry.getKey() ), entry.getValue() );
		}
		return result;
	}

	private Map<String, Set<Integer>> fetchChildren(IndexSearcher indexSearcher, Set<String> parentIds, Set<String> nestedDocumentPaths) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( String parentId : parentIds ) {
			TermQuery query = new TermQuery( new Term( LuceneFields.rootIdFieldName(), parentId ) );
			builder.add( query, BooleanClause.Occur.SHOULD );
		}
		builder.setMinimumNumberShouldMatch( 1 );
		builder.add( createNestedDocumentPathSubQuery( nestedDocumentPaths ), BooleanClause.Occur.MUST );
		BooleanQuery booleanQuery = builder.build();

		try {
			LuceneChildrenCollector childrenCollector = new LuceneChildrenCollector();
			indexSearcher.search( booleanQuery, childrenCollector );
			return childrenCollector.getChildren();
		}
		catch (IOException e) {
			throw log.errorFetchingNestedDocuments( booleanQuery, e );
		}
	}

	private BooleanQuery createNestedDocumentPathSubQuery(Set<String> nestedDocumentPaths) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( String nestedFieldPath : nestedDocumentPaths ) {
			TermQuery query = new TermQuery( new Term( LuceneFields.nestedDocumentPathFieldName(), nestedFieldPath ) );
			builder.add( query, BooleanClause.Occur.SHOULD );
		}
		builder.setMinimumNumberShouldMatch( 1 );
		return builder.build();
	}
}
