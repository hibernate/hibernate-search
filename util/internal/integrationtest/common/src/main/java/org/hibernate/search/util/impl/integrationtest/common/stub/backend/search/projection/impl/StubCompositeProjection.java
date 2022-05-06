/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.util.common.AssertionFailure;

class StubCompositeProjection<E, V> implements StubSearchProjection<V> {

	private final StubSearchProjection<?>[] inners;
	private final ProjectionCompositor<E, V> compositor;

	private StubCompositeProjection(StubSearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor) {
		this.inners = inners;
		this.compositor = compositor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + Arrays.toString( inners )
				+ ", compositor=" + compositor
				+ "]";
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		E extractedData = compositor.createInitial();

		Iterator<?> innerProjectionFromIndex = ( (List<?>) projectionFromIndex.next() ).iterator();
		for ( int i = 0; i < inners.length; i++ ) {
			Object extractedDataForInner = inners[i].extract( projectionHitMapper, innerProjectionFromIndex, context );
			extractedData = compositor.set( extractedData, i, extractedDataForInner );
		}

		return extractedData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V transform(LoadingResult<?, ?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		E transformedData = (E) extractedData;
		// Transform in-place
		for ( int i = 0; i < inners.length; i++ ) {
			Object extractedDataForInner = compositor.get( transformedData, i );
			Object transformedDataForInner = inners[i].transform( loadingResult, extractedDataForInner, context );
			transformedData = compositor.set( transformedData, i, transformedDataForInner );
		}

		return compositor.finish( transformedData );
	}

	static class Builder implements CompositeProjectionBuilder {

		Builder() {
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor,
				ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( !accumulatorProvider.isSingleValued() ) {
				throw new AssertionFailure( "Multi-valued projections are not supported in the stub backend." );
			}
			StubSearchProjection<?>[] typedInners =
					new StubSearchProjection<?>[ inners.length ];
			for ( int i = 0; i < inners.length; i++ ) {
				typedInners[i] = StubSearchProjection.from( inners[i] );
			}
			return (SearchProjection<P>) new StubCompositeProjection<>( typedInners, compositor );
		}
	}
}
