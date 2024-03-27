/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

public class ElasticsearchConstantProjection<T> extends AbstractElasticsearchProjection<T>
		implements ElasticsearchSearchProjection.Extractor<T, T> {
	private final T value;

	public ElasticsearchConstantProjection(ElasticsearchSearchIndexScope<?> scope, T value) {
		super( scope );
		this.value = value;
	}

	@Override
	public Extractor<?, T> request(JsonObject requestBody, ProjectionRequestContext context) {
		return this;
	}


	@Override
	public T extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit, JsonObject source,
			ProjectionExtractContext context) {
		return value;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, T extractedData, ProjectionTransformContext context) {
		return extractedData;
	}

}
