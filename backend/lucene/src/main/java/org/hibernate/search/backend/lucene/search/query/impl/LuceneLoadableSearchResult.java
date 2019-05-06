/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection.transformUnsafe;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionTransformContext;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;

/**
 * A search result from the backend that offers a method to load data from the mapper.
 * <p>
 * Allows to run loading in the user thread, and not in the backend HTTP request threads.
 * <p>
 * <strong>WARNING:</strong> loading should only be triggered once.
 * <p>
 * <strong>WARNING:</strong> this class is not thread-safe.
 *
 * @param <T> The type of hits in the search result.
 */
public class LuceneLoadableSearchResult<T> {
	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final LuceneSearchProjection<?, T> rootProjection;

	private final long hitCount;
	private List<Object> extractedData;

	LuceneLoadableSearchResult(ProjectionHitMapper<?, ?> projectionHitMapper,
			LuceneSearchProjection<?, T> rootProjection,
			long hitCount, List<Object> extractedData) {
		this.projectionHitMapper = projectionHitMapper;
		this.rootProjection = rootProjection;
		this.hitCount = hitCount;
		this.extractedData = extractedData;
	}

	long getHitCount() {
		return hitCount;
	}

	SearchResult<T> loadBlocking(SessionContextImplementor sessionContext) {
		SearchProjectionTransformContext transformContext = new SearchProjectionTransformContext( sessionContext );

		LoadingResult<?> loadingResult = projectionHitMapper.loadBlocking();

		for ( int i = 0; i < extractedData.size(); i++ ) {
			T transformed = transformUnsafe( rootProjection, loadingResult, extractedData.get( i ), transformContext );
			extractedData.set( i, transformed );
		}

		// The cast is safe, since all elements extend T and we make the list unmodifiable
		@SuppressWarnings("unchecked")
		List<T> loadedHits = Collections.unmodifiableList( (List<? extends T>) extractedData );

		// Make sure that if someone uses this object incorrectly, it will always fail, and will fail early.
		extractedData = null;

		return new SimpleSearchResult<>( hitCount, loadedHits );
	}
}
