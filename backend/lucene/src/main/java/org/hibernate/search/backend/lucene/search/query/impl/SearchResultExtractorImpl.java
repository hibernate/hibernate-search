/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.backend.lucene.search.extraction.impl.HitExtractor;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

public class SearchResultExtractorImpl<C, T> implements SearchResultExtractor<T> {

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;

	private final HitExtractor<? super C> hitExtractor;

	private final HitAggregator<C, List<T>> hitAggregator;

	public SearchResultExtractorImpl(
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.storedFieldVisitor = storedFieldVisitor;
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
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

	private List<T> extractHits(IndexSearcher indexSearcher, TopDocs topDocs) throws IOException {
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		hitAggregator.init( topDocs.scoreDocs.length );

		for ( ScoreDoc hit : topDocs.scoreDocs ) {
			indexSearcher.doc( hit.doc, storedFieldVisitor );
			Document document = storedFieldVisitor.getDocumentAndReset();

			C hitCollector = hitAggregator.nextCollector();
			hitExtractor.extract( hitCollector, document, hit.score );
		}

		return Collections.unmodifiableList( hitAggregator.build() );
	}
}
