/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection.transformUnsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchCompositeListProjection<T> implements ElasticsearchCompositeProjection<List<Object>, T> {

	private final Function<List<?>, T> transformer;

	private final List<ElasticsearchSearchProjection<?, ?>> children;

	public ElasticsearchCompositeListProjection(Function<List<?>, T> transformer,
			List<ElasticsearchSearchProjection<?, ?>> children) {
		this.transformer = transformer;
		this.children = children;
	}

	@Override
	public void contributeRequest(JsonObject requestBody,
			SearchProjectionExtractContext context) {
		for ( ElasticsearchSearchProjection<?, ?> child : children ) {
			child.contributeRequest( requestBody, context );
		}
	}

	@Override
	public List<Object> extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExtractContext context) {
		List<Object> extractedData = new ArrayList<>( children.size() );

		for ( ElasticsearchSearchProjection<?, ?> child : children ) {
			extractedData
					.add( child.extract( projectionHitMapper, responseBody, hit, context ) );
		}

		return extractedData;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, List<Object> extractedData,
			SearchProjectionTransformContext context) {
		for ( int i = 0; i < extractedData.size(); i++ ) {
			extractedData.set( i, transformUnsafe( children.get( i ), loadingResult, extractedData.get( i ), context ) );
		}

		return transformer.apply( extractedData );
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
