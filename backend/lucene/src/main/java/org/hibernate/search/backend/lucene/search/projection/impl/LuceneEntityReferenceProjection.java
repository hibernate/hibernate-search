/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.DocumentReferenceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;

public class LuceneEntityReferenceProjection<R> extends AbstractLuceneProjection<R, R> {

	private LuceneEntityReferenceProjection(LuceneSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireCollector( DocumentReferenceCollector.FACTORY );
	}

	@SuppressWarnings("unchecked")
	@Override
	public R extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		DocumentReference documentReference =
				context.getCollector( DocumentReferenceCollector.KEY ).get( documentResult.getDocId() );
		return (R) mapper.convertReference( documentReference );
	}

	@Override
	public R transform(LoadingResult<?> loadingResult, R extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	public static class Builder<R> extends AbstractLuceneProjection.AbstractBuilder<R>
			implements EntityReferenceProjectionBuilder<R> {

		public Builder(LuceneSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public SearchProjection<R> build() {
			return new LuceneEntityReferenceProjection<>( searchContext );
		}
	}
}
