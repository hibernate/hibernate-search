/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchCompositeFunctionProjection<E, P1, P> implements ElasticsearchCompositeProjection<E, P> {

	private final Set<String> indexNames;

	private final Function<P1, P> transformer;

	private final ElasticsearchSearchProjection<E, P1> projection;

	public ElasticsearchCompositeFunctionProjection(Set<String> indexNames, Function<P1, P> transformer,
			ElasticsearchSearchProjection<E, P1> projection) {
		this.indexNames = indexNames;
		this.transformer = transformer;
		this.projection = projection;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExtractContext context) {
		projection.contributeRequest( requestBody, context );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		return projection.extract( projectionHitMapper, responseBody, hit, context );
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, E extractedData, SearchProjectionTransformContext context) {
		return transformer.apply( projection.transform( loadingResult, extractedData, context ) );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
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
