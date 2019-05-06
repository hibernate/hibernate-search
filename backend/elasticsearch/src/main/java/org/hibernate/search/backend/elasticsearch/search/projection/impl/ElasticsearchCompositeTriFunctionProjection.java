/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection.transformUnsafe;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.function.TriFunction;

import com.google.gson.JsonObject;

public class ElasticsearchCompositeTriFunctionProjection<P1, P2, P3, T> implements
		ElasticsearchCompositeProjection<Object[], T> {

	private final TriFunction<P1, P2, P3, T> transformer;

	private final ElasticsearchSearchProjection<?, P1> projection1;

	private final ElasticsearchSearchProjection<?, P2> projection2;

	private final ElasticsearchSearchProjection<?, P3> projection3;

	public ElasticsearchCompositeTriFunctionProjection(TriFunction<P1, P2, P3, T> transformer,
			ElasticsearchSearchProjection<?, P1> projection1, ElasticsearchSearchProjection<?, P2> projection2,
			ElasticsearchSearchProjection<?, P3> projection3) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
		this.projection3 = projection3;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExtractContext context) {
		projection1.contributeRequest( requestBody, context );
		projection2.contributeRequest( requestBody, context );
		projection3.contributeRequest( requestBody, context );
	}

	@Override
	public Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		return new Object[] {
				projection1.extract( projectionHitMapper, responseBody, hit, context ),
				projection2.extract( projectionHitMapper, responseBody, hit, context ),
				projection3.extract( projectionHitMapper, responseBody, hit, context )
		};
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object[] extractedData,
			SearchProjectionTransformContext context) {
		return transformer.apply(
				transformUnsafe( projection1, loadingResult, extractedData[0], context ),
				transformUnsafe( projection2, loadingResult, extractedData[1], context ),
				transformUnsafe( projection3, loadingResult, extractedData[2], context )
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
