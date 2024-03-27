/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionIndexScope;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.AbstractSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.dsl.spi.StaticProjectionFinalStep;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionFactoryImpl<R, E>
		extends AbstractSearchProjectionFactory<
				ElasticsearchSearchProjectionFactory<R, E>,
				ElasticsearchSearchProjectionIndexScope<?>,
				R,
				E>
		implements ElasticsearchSearchProjectionFactory<R, E> {

	public ElasticsearchSearchProjectionFactoryImpl(
			SearchProjectionDslContext<ElasticsearchSearchProjectionIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public ElasticsearchSearchProjectionFactory<R, E> withRoot(String objectFieldPath) {
		return new ElasticsearchSearchProjectionFactoryImpl<>( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@Override
	public ProjectionFinalStep<JsonObject> source() {
		return new StaticProjectionFinalStep<>( dslContext.scope().projectionBuilders().source() );
	}

	@Override
	public ProjectionFinalStep<JsonObject> explanation() {
		return new StaticProjectionFinalStep<>( dslContext.scope().projectionBuilders().explanation() );
	}

	@Override
	public ProjectionFinalStep<JsonObject> jsonHit() {
		return new StaticProjectionFinalStep<>( dslContext.scope().projectionBuilders().jsonHit() );
	}
}
