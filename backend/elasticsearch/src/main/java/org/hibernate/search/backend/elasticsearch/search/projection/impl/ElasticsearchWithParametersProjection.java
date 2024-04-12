/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;

import com.google.gson.JsonObject;

final class ElasticsearchWithParametersProjection<P>
		extends AbstractElasticsearchProjection<P> {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final Function<? super NamedValues,
			? extends ProjectionFinalStep<P>> projectionCreator;

	public ElasticsearchWithParametersProjection(ElasticsearchSearchIndexScope<?> scope,
			Function<? super NamedValues, ? extends ProjectionFinalStep<P>> projectionCreator) {
		super( scope );
		this.scope = scope;
		this.projectionCreator = projectionCreator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "projectionCreator=" + projectionCreator
				+ "]";
	}

	@Override
	public Extractor<?, P> request(JsonObject requestBody, ProjectionRequestContext context) {
		SearchProjection<P> delegate = projectionCreator.apply( context.queryParameters() )
				.toProjection();
		return ElasticsearchSearchProjection.from( scope, delegate ).request( requestBody, context );
	}

}
