/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection.transformUnsafe;

import java.util.function.BiFunction;

import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchCompositeBiFunctionProjection<P1, P2, T> implements
		ElasticsearchCompositeProjection<Object[], T> {

	private final BiFunction<P1, P2, T> transformer;

	private final ElasticsearchSearchProjection<?, P1> projection1;

	private final ElasticsearchSearchProjection<?, P2> projection2;

	public ElasticsearchCompositeBiFunctionProjection(BiFunction<P1, P2, T> transformer,
			ElasticsearchSearchProjection<?, P1> projection1, ElasticsearchSearchProjection<?, P2> projection2) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		projection1.contributeRequest( requestBody, searchProjectionExecutionContext );
		projection2.contributeRequest( requestBody, searchProjectionExecutionContext );
	}

	@Override
	public Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		return new Object[] {
				projection1.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext ),
				projection2.extract( projectionHitMapper, responseBody, hit, searchProjectionExecutionContext )
		};
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object[] extractedData) {
		return transformer.apply(
				transformUnsafe( projection1, loadingResult, extractedData[0] ),
				transformUnsafe( projection2, loadingResult, extractedData[1] )
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
