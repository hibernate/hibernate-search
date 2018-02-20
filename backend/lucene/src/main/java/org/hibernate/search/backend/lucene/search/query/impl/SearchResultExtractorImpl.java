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

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

public class SearchResultExtractorImpl<C, T> implements SearchResultExtractor<T> {

	private final HitExtractor<? super C> hitExtractor;
	private final HitAggregator<C, List<T>> hitAggregator;

	public SearchResultExtractorImpl(
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
	}

	@Override
	public SearchResult<T> extract(IndexSearcher indexSearcher, TopDocs topDocs) throws IOException {
		hitAggregator.init( topDocs.scoreDocs.length );

		for ( ScoreDoc hit : topDocs.scoreDocs ) {
			C hitCollector = hitAggregator.nextCollector();
			hitExtractor.extract( hitCollector, indexSearcher, hit );
		}

		long totalHits = topDocs.totalHits;

		final List<T> finalHits = Collections.unmodifiableList( hitAggregator.build() );
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
}
