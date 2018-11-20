/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection.transformUnsafe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExecutionContext;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

class SearchResultExtractorImpl<T> implements SearchResultExtractor<T> {

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final LuceneSearchProjection<?, T> rootProjection;
	private final ProjectionHitMapper<?, ?> projectionHitMapper;

	private final SearchProjectionExecutionContext searchProjectionExecutionContext;

	SearchResultExtractorImpl(
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			LuceneSearchProjection<?, T> rootProjection,
			ProjectionHitMapper<?, ?> projectionHitMapper,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		this.storedFieldVisitor = storedFieldVisitor;
		this.rootProjection = rootProjection;
		this.projectionHitMapper = projectionHitMapper;
		this.searchProjectionExecutionContext = searchProjectionExecutionContext;
	}

	@Override
	public SearchResult<T> extract(IndexSearcher indexSearcher, long totalHits, TopDocs topDocs) throws IOException {
		List<T> finalHits = extractHits( indexSearcher, topDocs );

		return new SearchResult<T>() {

			@Override
			public long getHitCount() {
				return totalHits;
			}

			@Override
			public List<T> getHits() {
				return finalHits;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private List<T> extractHits(IndexSearcher indexSearcher, TopDocs topDocs) throws IOException {
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		List<Object> hits = new ArrayList<>( topDocs.scoreDocs.length );

		for ( ScoreDoc hit : topDocs.scoreDocs ) {
			indexSearcher.doc( hit.doc, storedFieldVisitor );
			Document document = storedFieldVisitor.getDocumentAndReset();
			LuceneResult luceneResult = new LuceneResult( document, hit.doc, hit.score );

			hits.add( rootProjection.extract( projectionHitMapper, luceneResult, searchProjectionExecutionContext ) );
		}

		LoadingResult<?> loadingResult = projectionHitMapper.load();

		for ( int i = 0; i < hits.size(); i++ ) {
			hits.set( i, transformUnsafe( rootProjection, loadingResult, hits.get( i ) ) );
		}

		return Collections.unmodifiableList( (List<T>) hits );
	}
}
