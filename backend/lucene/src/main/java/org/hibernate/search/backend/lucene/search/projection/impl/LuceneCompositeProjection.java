/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Arrays;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;

class LuceneCompositeProjection<E, V>
		extends AbstractLuceneProjection<E, V> {

	private final ProjectionCompositor<E, V> compositor;
	private final LuceneSearchProjection<?, ?>[] inners;

	private LuceneCompositeProjection(Builder<E, V> builder) {
		super( builder.scope );
		this.compositor = builder.compositor;
		this.inners = builder.inners;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + Arrays.toString( inners )
				+ ", compositor=" + compositor
				+ "]";
	}

	@Override
	public void request(ProjectionRequestContext context) {
		for ( LuceneSearchProjection<?, ?> inner : inners ) {
			inner.request( context );
		}
	}

	@Override
	public final E extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			ProjectionExtractContext context) {
		E extractedData = compositor.createInitial();

		for ( int i = 0; i < inners.length; i++ ) {
			Object extractedDataForInner = inners[i].extract( mapper, documentResult, context );
			extractedData = compositor.set( extractedData, i, extractedDataForInner );
		}

		return extractedData;
	}

	@Override
	public final V transform(LoadingResult<?, ?> loadingResult, E extractedData,
			ProjectionTransformContext context) {
		E transformedData = extractedData;
		// Transform in-place
		for ( int i = 0; i < inners.length; i++ ) {
			Object extractedDataForInner = compositor.get( transformedData, i );
			Object transformedDataForInner = LuceneSearchProjection.transformUnsafe( inners[i], loadingResult,
					extractedDataForInner, context );
			transformedData = compositor.set( transformedData, i, transformedDataForInner );
		}

		return compositor.finish( transformedData );
	}

	static class Builder<E, V> implements CompositeProjectionBuilder<V> {

		private final LuceneSearchIndexScope<?> scope;
		private final ProjectionCompositor<E, V> compositor;
		private final LuceneSearchProjection<?, ?>[] inners;

		Builder(LuceneSearchIndexScope<?> scope, ProjectionCompositor<E, V> compositor,
				LuceneSearchProjection<?, ?> ... inners) {
			this.scope = scope;
			this.compositor = compositor;
			this.inners = inners;
		}

		@Override
		public SearchProjection<V> build() {
			return new LuceneCompositeProjection<>( this );
		}
	}
}
