/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.IdProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

public final class IdProjectionOptionsStepImpl<I> implements IdProjectionOptionsStep<IdProjectionOptionsStepImpl<I>, I> {

	private final SearchProjection<I> idProjection;

	public IdProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext, Class<I> identifierType) {
		idProjection = dslContext.scope().projectionBuilders().id( identifierType );
	}

	@Override
	public SearchProjection<I> toProjection() {
		return idProjection;
	}
}
