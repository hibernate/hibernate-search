/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.List;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.util.function.TriFunction;

public class StubCompositeTriFunctionSearchProjection<P1, P2, P3, T> implements StubCompositeSearchProjection<T> {

	private final TriFunction<P1, P2, P3, T> transformer;

	private final StubSearchProjection<P1> projection1;

	private final StubSearchProjection<P2> projection2;

	private final StubSearchProjection<P3> projection3;

	public StubCompositeTriFunctionSearchProjection(TriFunction<P1, P2, P3, T> transformer,
			StubSearchProjection<P1> projection1, StubSearchProjection<P2> projection2,
			StubSearchProjection<P3> projection3) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
		this.projection3 = projection3;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			FromDocumentFieldValueConvertContext context) {
		List<?> listFromIndex = (List<?>) projectionFromIndex;

		Object[] extractedElements = new Object[] {
				projection1.extract( projectionHitMapper, listFromIndex.get( 0 ), context ),
				projection2.extract( projectionHitMapper, listFromIndex.get( 1 ), context ),
				projection3.extract( projectionHitMapper, listFromIndex.get( 2 ), context )
		};
		return extractedElements;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		Object[] extractedElements = (Object[]) extractedData;

		return transformer.apply(
				projection1.transform( loadingResult, extractedElements[0] ),
				projection2.transform( loadingResult, extractedElements[0] ),
				projection3.transform( loadingResult, extractedElements[0] )
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "projection1=" ).append( projection1 )
				.append( ", projection2=" ).append( projection2 )
				.append( ", projection3=" ).append( projection3 )
				.append( "]" );
		return sb.toString();
	}
}
