/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class LuceneEntityProjection<E> implements LuceneSearchProjection<Object, E> {

	public LuceneEntityProjection() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		DocumentReferenceExtractorHelper.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		DocumentReferenceExtractorHelper.contributeFields( builder );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return mapper.planLoading( DocumentReferenceExtractorHelper.extractDocumentReference( documentResult ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public E transform(LoadingResult<?> loadingResult, Object extractedData,
			SearchProjectionTransformContext context) {
		return (E) loadingResult.getLoaded( extractedData );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
