/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class StubCompositeListProjection<P> implements StubCompositeProjection<P> {

	private final Function<List<?>, P> transformer;

	private final List<StubSearchProjection<?>> children;

	StubCompositeListProjection(Function<List<?>, P> transformer,
			List<StubSearchProjection<?>> children) {
		this.transformer = transformer;
		this.children = children;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		List<Object> extractedData = new ArrayList<>();
		List<?> listFromIndex = (List<?>) projectionFromIndex;

		for ( int i = 0; i < listFromIndex.size(); i++ ) {
			extractedData.add( children.get( i ).extract( projectionHitMapper, listFromIndex.get( i ), context ) );
		}

		return extractedData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public P transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		List<Object> extractedElements = (List<Object>) extractedData;
		List<Object> results = new ArrayList<>( extractedElements.size() );

		for ( int i = 0; i < extractedElements.size(); i++ ) {
			results.add( i, children.get( i ).transform( loadingResult, extractedElements.get( i ), context ) );
		}

		return transformer.apply( results );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "children=" ).append( children )
				.append( "]" );
		return sb.toString();
	}
}
