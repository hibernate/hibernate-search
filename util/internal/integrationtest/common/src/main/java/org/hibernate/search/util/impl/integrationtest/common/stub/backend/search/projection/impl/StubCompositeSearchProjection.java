/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

public class StubCompositeSearchProjection<T> implements StubSearchProjection<T> {

	private final List<StubSearchProjection<?>> children;

	public StubCompositeSearchProjection(List<StubSearchProjection<?>> children) {
		this.children = children;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			FromIndexFieldValueConvertContext context) {
		List<Object> extractedData = new ArrayList<>();
		List<Object> listFromIndex = (List<Object>) projectionFromIndex;

		for ( int i = 0; i < listFromIndex.size(); i++ ) {
			extractedData.add( children.get( i ).extract( projectionHitMapper, listFromIndex.get( i ), context ) );
		}

		return extractedData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		List<Object> extractedElements = (List<Object>) extractedData;
		List<Object> results = new ArrayList<>( extractedElements.size() );

		for ( int i = 0; i < extractedElements.size(); i++ ) {
			results.add( i, children.get( i ).transform( loadingResult, extractedElements.get( i ) ) );
		}

		return (T) results;
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
