/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class CompositeListSearchProjectionImpl<T> implements CompositeSearchProjection<T> {

	private final Function<List<?>, T> transformer;

	private final List<ElasticsearchSearchProjection<?>> children;

	public CompositeListSearchProjectionImpl(Function<List<?>, T> transformer,
			List<ElasticsearchSearchProjection<?>> children) {
		this.transformer = transformer;
		this.children = children;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		for ( ElasticsearchSearchProjection<?> child : children ) {
			child.contributeRequest( requestBody, searchProjectionExecutionContext );
		}
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		List<Object> extractedData = new ArrayList<>( children.size() );

		for ( ElasticsearchSearchProjection<?> child : children ) {
			extractedData.add( child.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext ) );
		}

		return extractedData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		List<Object> extractedElements = (List<Object>) extractedData;

		for ( int i = 0; i < extractedElements.size(); i++ ) {
			extractedElements.set( i, children.get( i ).transform( loadingResult, extractedElements.get( i ) ) );
		}

		return transformer.apply( extractedElements );
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
