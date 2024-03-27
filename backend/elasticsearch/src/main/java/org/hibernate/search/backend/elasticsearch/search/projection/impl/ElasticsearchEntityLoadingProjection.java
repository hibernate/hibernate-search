/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchSearchHints;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

import com.google.gson.JsonObject;

public class ElasticsearchEntityLoadingProjection<E> extends AbstractElasticsearchProjection<E>
		implements ElasticsearchSearchProjection.Extractor<Object, E> {

	private final DocumentReferenceExtractionHelper helper;

	ElasticsearchEntityLoadingProjection(ElasticsearchSearchIndexScope<?> scope,
			DocumentReferenceExtractionHelper helper) {
		super( scope );
		this.helper = helper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, E> request(JsonObject requestBody, ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.ENTITY,
				ElasticsearchSearchHints.INSTANCE.entityProjectionNestingNotSupportedHint()
		);
		helper.request( requestBody, context );
		return this;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
			JsonObject source, ProjectionExtractContext context) {
		return projectionHitMapper.planLoading( helper.extract( hit, context ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public E transform(LoadingResult<?> loadingResult, Object extractedData,
			ProjectionTransformContext context) {
		E loaded = (E) loadingResult.get( extractedData );
		if ( loaded == null ) {
			context.reportFailedLoad();
		}
		return loaded;
	}

}
