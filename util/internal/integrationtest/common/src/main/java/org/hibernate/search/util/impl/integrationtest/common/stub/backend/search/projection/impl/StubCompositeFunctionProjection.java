/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class StubCompositeFunctionProjection<P1, P> implements StubCompositeProjection<P> {

	private final Function<P1, P> transformer;

	private final StubSearchProjection<P1> projection;

	StubCompositeFunctionProjection(Function<P1, P> transformer,
			StubSearchProjection<P1> projection) {
		this.transformer = transformer;
		this.projection = projection;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		return projection.extract( projectionHitMapper, projectionFromIndex, context );
	}

	@Override
	public P transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return transformer.apply( projection.transform( loadingResult, extractedData, context ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "projection=" ).append( projection )
				.append( "]" );
		return sb.toString();
	}
}
