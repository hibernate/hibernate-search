/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchSearchHints;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

import com.google.gson.JsonObject;

public class ElasticsearchEntityCompositeProjection<E> extends AbstractElasticsearchProjection<E> {
	private final ElasticsearchSearchProjection<E> delegate;

	public ElasticsearchEntityCompositeProjection(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchProjection<E> delegate) {
		super( scope );
		this.delegate = delegate;
	}

	@Override
	public Extractor<?, E> request(JsonObject requestBody, ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.ENTITY,
				ElasticsearchSearchHints.INSTANCE.entityProjectionNestingNotSupportedHint()
		);
		return delegate.request( requestBody, context );
	}
}
