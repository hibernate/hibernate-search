/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection.transformUnsafe;

import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchCompositeBiFunctionProjection<P1, P2, P> implements
		ElasticsearchCompositeProjection<Object[], P> {

	private final Set<String> indexNames;

	private final BiFunction<P1, P2, P> transformer;

	private final ElasticsearchSearchProjection<?, P1> projection1;
	private final ElasticsearchSearchProjection<?, P2> projection2;

	public ElasticsearchCompositeBiFunctionProjection(Set<String> indexNames, BiFunction<P1, P2, P> transformer,
			ElasticsearchSearchProjection<?, P1> projection1, ElasticsearchSearchProjection<?, P2> projection2) {
		this.indexNames = indexNames;
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExtractContext context) {
		projection1.contributeRequest( requestBody, context );
		projection2.contributeRequest( requestBody, context );
	}

	@Override
	public Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		return new Object[] {
				projection1.extract( projectionHitMapper, responseBody, hit, context ),
				projection2.extract( projectionHitMapper, responseBody, hit, context )
		};
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, Object[] extractedData,
			SearchProjectionTransformContext context) {
		return transformer.apply(
				transformUnsafe( projection1, loadingResult, extractedData[0], context ),
				transformUnsafe( projection2, loadingResult, extractedData[1], context )
		);
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
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
