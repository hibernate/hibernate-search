/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class CompositeFunctionSearchProjectionImpl<P, T> implements CompositeSearchProjection<T> {

	private final Function<P, T> transformer;

	private final ElasticsearchSearchProjection<P> projection;

	public CompositeFunctionSearchProjectionImpl(Function<P, T> transformer,
			ElasticsearchSearchProjection<P> projection) {
		this.transformer = transformer;
		this.projection = projection;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		projection.contributeRequest( requestBody, searchProjectionExecutionContext );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		return projection.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext );
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		return transformer.apply( projection.transform( loadingResult, extractedData ) );
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
