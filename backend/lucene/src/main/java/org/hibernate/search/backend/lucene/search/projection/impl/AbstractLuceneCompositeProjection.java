/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Arrays;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;

abstract class AbstractLuceneCompositeProjection<P>
		extends AbstractLuceneProjection<Object[], P> {

	private final LuceneSearchProjection<?, ?>[] children;

	AbstractLuceneCompositeProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchProjection<?, ?> ... children) {
		super( scope );
		this.children = children;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "children=" + Arrays.toString( children )
				+ "]";
	}

	@Override
	public void request(ProjectionRequestContext context) {
		for ( LuceneSearchProjection<?, ?> child : children ) {
			child.request( context );
		}
	}

	@Override
	public final Object[] extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			ProjectionExtractContext context) {
		Object[] extractedData = new Object[children.length];

		for ( int i = 0; i < extractedData.length; i++ ) {
			LuceneSearchProjection<?, ?> child = children[i];
			extractedData[i] = child.extract(
					mapper, documentResult, context
			);
		}

		return extractedData;
	}

	@Override
	public final P transform(LoadingResult<?, ?> loadingResult, Object[] extractedData,
			ProjectionTransformContext context) {
		// Transform in-place
		for ( int i = 0; i < extractedData.length; i++ ) {
			LuceneSearchProjection<?, ?> child = children[i];
			Object extractedElement = extractedData[i];
			extractedData[i] = LuceneSearchProjection.transformUnsafe(
					child, loadingResult, extractedElement, context
			);
		}

		return doTransform( extractedData );
	}

	/**
	 * @param childResults An object array guaranteed to contain
	 * the result of calling {@link LuceneSearchProjection#extract(ProjectionHitMapper, LuceneResult, ProjectionExtractContext)},
	 * then {@link LuceneSearchProjection#transform(LoadingResult, Object, ProjectionTransformContext)},
	 * for each child projection.
	 * Each result has the same index as the child projection it originated from.
	 * @return The combination of the child results to return from {@link #transform(LoadingResult, Object[], ProjectionTransformContext)}.
	 */
	abstract P doTransform(Object[] childResults);

	static class Builder<P> implements CompositeProjectionBuilder<P> {

		private final AbstractLuceneCompositeProjection<P> projection;

		Builder(AbstractLuceneCompositeProjection<P> projection) {
			this.projection = projection;
		}

		@Override
		public SearchProjection<P> build() {
			return projection;
		}
	}
}
