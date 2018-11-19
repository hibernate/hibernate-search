/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.util.function.TriFunction;

import com.google.gson.JsonObject;

public class CompositeTriFunctionSearchProjectionImpl<P1, P2, P3, T> implements CompositeSearchProjection<T> {

	private final TriFunction<P1, P2, P3, T> transformer;

	private final ElasticsearchSearchProjection<P1> projection1;

	private final ElasticsearchSearchProjection<P2> projection2;

	private final ElasticsearchSearchProjection<P3> projection3;

	public CompositeTriFunctionSearchProjectionImpl(TriFunction<P1, P2, P3, T> transformer,
			ElasticsearchSearchProjection<P1> projection1, ElasticsearchSearchProjection<P2> projection2,
			ElasticsearchSearchProjection<P3> projection3) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
		this.projection3 = projection3;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		projection1.contributeRequest( requestBody, searchProjectionExecutionContext );
		projection2.contributeRequest( requestBody, searchProjectionExecutionContext );
		projection3.contributeRequest( requestBody, searchProjectionExecutionContext );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		return new Object[] {
				projection1.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext ),
				projection2.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext ),
				projection3.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext )
		};
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		Object[] extractedElements = (Object[]) extractedData;

		return transformer.apply(
				projection1.transform( loadingResult, extractedElements[0] ),
				projection2.transform( loadingResult, extractedElements[1] ),
				projection3.transform( loadingResult, extractedElements[2] )
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
