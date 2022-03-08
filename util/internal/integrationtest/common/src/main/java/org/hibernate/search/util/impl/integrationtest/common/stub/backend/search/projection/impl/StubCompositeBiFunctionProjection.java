/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class StubCompositeBiFunctionProjection<P1, P2, P> implements StubCompositeProjection<P> {

	private final BiFunction<P1, P2, P> transformer;

	private final StubSearchProjection<P1> projection1;

	private final StubSearchProjection<P2> projection2;

	StubCompositeBiFunctionProjection(BiFunction<P1, P2, P> transformer,
			StubSearchProjection<P1> projection1, StubSearchProjection<P2> projection2) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		List<?> listFromIndex = (List<?>) projectionFromIndex;

		return new Object[] {
				projection1.extract( projectionHitMapper, listFromIndex.get( 0 ), context ),
				projection2.extract( projectionHitMapper, listFromIndex.get( 1 ), context )
		};
	}

	@Override
	public P transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		Object[] extractedElements = (Object[]) extractedData;

		return transformer.apply(
				projection1.transform( loadingResult, extractedElements[0], context ),
				projection2.transform( loadingResult, extractedElements[1], context )
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "projection1=" ).append( projection1 )
				.append( ", projection2=" ).append( projection2 )
				.append( "]" );
		return sb.toString();
	}
}
