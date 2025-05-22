/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition;

import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link ProjectionDefinition#create(ProjectionDefinitionContext)}.
 * @see ProjectionDefinition#create(ProjectionDefinitionContext)
 */
@Incubating
public interface ProjectionDefinitionContext {
	/**
	 * @return A projection factory.
	 * If the projection is used in the context of an object field,
	 * this factory expects field paths to be provided relative to that same object field.
	 * This factory is only valid in the present context and must not be used after
	 * {@link ProjectionDefinition#create(ProjectionDefinitionContext)} returns.
	 */
	SearchProjectionFactory<?, ?> projection();

	ProjectionDefinitionContext withRoot(String fieldPath);
}
