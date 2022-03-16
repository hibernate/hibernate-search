/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.DocumentReferenceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;

public class LuceneEntityProjection<E> extends AbstractLuceneProjection<E>
		implements LuceneSearchProjection.Extractor<Object, E> {

	private LuceneEntityProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, E> request(ProjectionRequestContext context) {
		context.requireCollector( DocumentReferenceCollector.FACTORY );
		return this;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			ProjectionExtractContext context) {
		DocumentReference documentReference =
				context.getCollector( DocumentReferenceCollector.KEY ).get( documentResult.getDocId() );
		return mapper.planLoading( documentReference );
	}

	@SuppressWarnings("unchecked")
	@Override
	public E transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			ProjectionTransformContext context) {
		E loaded = (E) loadingResult.get( extractedData );
		if ( loaded == null ) {
			context.reportFailedLoad();
		}
		return loaded;
	}

	public static class Builder<E> extends AbstractLuceneProjection.AbstractBuilder<E>
			implements EntityProjectionBuilder<E> {

		public Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public SearchProjection<E> build() {
			return new LuceneEntityProjection<>( scope );
		}
	}
}
