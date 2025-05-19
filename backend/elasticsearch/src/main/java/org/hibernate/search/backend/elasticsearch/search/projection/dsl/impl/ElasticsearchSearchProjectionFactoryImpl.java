/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionIndexScope;
import org.hibernate.search.engine.search.projection.dsl.ExtendedSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.AbstractSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.dsl.spi.StaticProjectionFinalStep;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionFactoryImpl<SR, R, E>
		extends AbstractSearchProjectionFactory<
				SR,
				ElasticsearchSearchProjectionFactory<SR, R, E>,
				ElasticsearchSearchProjectionIndexScope<?>,
				R,
				E>
		implements ElasticsearchSearchProjectionFactory<SR, R, E> {

	public ElasticsearchSearchProjectionFactoryImpl(
			SearchProjectionDslContext<ElasticsearchSearchProjectionIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public ElasticsearchSearchProjectionFactory<SR, R, E> withRoot(String objectFieldPath) {
		return new ElasticsearchSearchProjectionFactoryImpl<>( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@Override
	public <SR2> ExtendedSearchProjectionFactory<SR2, ?, R, E> withScopeRoot(Class<SR2> scopeRootType) {
		return new ElasticsearchSearchProjectionFactoryImpl<>( dslContext );
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
