package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchConstantProjection<T> extends AbstractElasticsearchProjection<T>
		implements ElasticsearchSearchProjection.Extractor<T, T> {
	private final T value;

	public ElasticsearchConstantProjection(ElasticsearchSearchIndexScope<?> scope, T value) {
		super( scope );
		this.value = value;
	}

	@Override
	public Extractor<?, T> request(ObjectNode requestBody, ProjectionRequestContext context) {
		return this;
	}


	@Override
	public T extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit, ObjectNode source,
			ProjectionExtractContext context) {
		return value;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, T extractedData, ProjectionTransformContext context) {
		return extractedData;
	}

}
