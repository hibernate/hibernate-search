/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;

public interface CompositeProjectionDefinition<T> extends ProjectionDefinition<T>, AutoCloseable {

	@Override
	default SearchProjection<? extends T> create(SearchProjectionFactory<?, ?> factory,
			ProjectionDefinitionContext context) {
		return apply( factory, factory.composite(), context ).toProjection();
	}

	CompositeProjectionValueStep<?, T> apply(SearchProjectionFactory<?, ?> projectionFactory,
			CompositeProjectionInnerStep initialStep,
			ProjectionDefinitionContext context);

	/**
	 * Close any resource before the projection definition is discarded.
	 */
	@Override
	default void close() {
		// Do nothing by default
	}

}
