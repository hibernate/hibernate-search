/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.util.common.AssertionFailure;

final class StubWithParametersProjection<P> extends StubSearchProjection<P> {

	private final StubSearchProjection<P> delegate;

	StubWithParametersProjection(Function<? super NamedValues, ? extends ProjectionFinalStep<P>> projectionCreator) {

		delegate = (StubSearchProjection<P>) projectionCreator.apply( new NamedValues() {

			@Override
			public <T> T get(String name, Class<T> paramType) {
				throw new AssertionFailure( "Stub mapper does not support query parameters" );
			}

			@Override
			public <T> Optional<T> getOptional(String name, Class<T> paramType) {
				throw new AssertionFailure( "Stub mapper does not support query parameters" );
			}
		} ).toProjection();
	}


	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return delegate.extract( projectionHitMapper, projectionFromIndex, context );
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		return delegate.transform( loadingResult, extractedData, context );
	}

	@Override
	protected String typeName() {
		return delegate.typeName();
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		delegate.toNode( self );
	}
}
