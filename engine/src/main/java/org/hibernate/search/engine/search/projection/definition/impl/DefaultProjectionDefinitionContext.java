/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.impl;

import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;

public record DefaultProjectionDefinitionContext(SearchProjectionFactory<?, ?> projection)
		implements ProjectionDefinitionContext {

	@Override
	public ProjectionDefinitionContext withRoot(String fieldPath) {
		return new DefaultProjectionDefinitionContext( projection.withRoot( fieldPath ) );
	}
}
