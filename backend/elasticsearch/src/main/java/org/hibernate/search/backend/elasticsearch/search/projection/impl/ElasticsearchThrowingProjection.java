package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.SearchException;

import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchThrowingProjection<T> extends AbstractElasticsearchProjection<T>
		implements ElasticsearchSearchProjection.Extractor<Object, T> {
	private final Supplier<SearchException> exceptionSupplier;

	public ElasticsearchThrowingProjection(ElasticsearchSearchIndexScope<?> scope,
			Supplier<SearchException> exceptionSupplier) {
		super( scope );
		this.exceptionSupplier = exceptionSupplier;
	}

	@Override
	public Extractor<?, T> request(ObjectNode requestBody, ProjectionRequestContext context) {
		return this;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit, ObjectNode source,
			ProjectionExtractContext context) {
		throw exceptionSupplier.get();
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData, ProjectionTransformContext context) {
		throw exceptionSupplier.get();
	}
}
