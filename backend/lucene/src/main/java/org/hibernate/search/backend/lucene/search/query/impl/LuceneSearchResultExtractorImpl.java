/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReusableDocumentStoredFieldVisitor;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class LuceneSearchResultExtractorImpl<T> implements LuceneSearchResultExtractor<T> {

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final LuceneSearchProjection<?, T> rootProjection;
	private final ProjectionHitMapper<?, ?> projectionHitMapper;

	LuceneSearchResultExtractorImpl(
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			LuceneSearchProjection<?, T> rootProjection,
			ProjectionHitMapper<?, ?> projectionHitMapper) {
		this.storedFieldVisitor = storedFieldVisitor;
		this.rootProjection = rootProjection;
		this.projectionHitMapper = projectionHitMapper;
	}

	@Override
	public LuceneLoadableSearchResult<T> extract(IndexSearcher indexSearcher, long totalHits, TopDocs topDocs,
			SearchProjectionExtractContext projectionExecutionContext) throws IOException {
		List<Object> extractedData = extractHits( indexSearcher, topDocs, projectionExecutionContext );

		return new LuceneLoadableSearchResult<>(
				projectionHitMapper, rootProjection,
				totalHits, extractedData
		);
	}

	private List<Object> extractHits(IndexSearcher indexSearcher, TopDocs topDocs,
			SearchProjectionExtractContext projectionExecutionContext) throws IOException {
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		List<Object> extractedData = new ArrayList<>( topDocs.scoreDocs.length );

		for ( ScoreDoc hit : topDocs.scoreDocs ) {
			indexSearcher.doc( hit.doc, storedFieldVisitor );
			Document document = storedFieldVisitor.getDocumentAndReset();
			LuceneResult luceneResult = new LuceneResult( document, hit.doc, hit.score );

			extractedData.add( rootProjection.extract( projectionHitMapper, luceneResult, projectionExecutionContext ) );
		}

		return extractedData;
	}
}
