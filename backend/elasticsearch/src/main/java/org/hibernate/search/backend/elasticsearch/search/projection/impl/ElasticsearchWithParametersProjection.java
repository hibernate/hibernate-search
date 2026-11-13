package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;

import tools.jackson.databind.node.ObjectNode;

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
	public Extractor<?, P> request(ObjectNode requestBody, ProjectionRequestContext context) {
		SearchProjection<P> delegate = projectionCreator.apply( context.queryParameters() )
				.toProjection();
		return ElasticsearchSearchProjection.from( scope, delegate ).request( requestBody, context );
	}

}
