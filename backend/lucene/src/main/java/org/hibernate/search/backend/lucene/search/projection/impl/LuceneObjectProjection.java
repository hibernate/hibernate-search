/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

public class LuceneObjectProjection<O> implements LuceneSearchProjection<Object, O> {

	@SuppressWarnings("rawtypes")
	private static final LuceneObjectProjection INSTANCE = new LuceneObjectProjection();

	@SuppressWarnings("unchecked")
	public static <T> LuceneObjectProjection<T> get() {
		return INSTANCE;
	}

	private LuceneObjectProjection() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		DocumentReferenceExtractorHelper.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		DocumentReferenceExtractorHelper.contributeFields( absoluteFieldPaths );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExecutionContext context) {
		return mapper.planLoading( DocumentReferenceExtractorHelper.extractDocumentReference( documentResult ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public O transform(LoadingResult<?> loadingResult, Object extractedData) {
		return (O) loadingResult.getLoaded( extractedData );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
