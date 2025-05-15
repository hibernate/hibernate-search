/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition;

import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link ProjectionDefinition#create(TypedSearchProjectionFactory, ProjectionDefinitionContext)}.
 * @see ProjectionDefinition#create(TypedSearchProjectionFactory, ProjectionDefinitionContext)
 */
@Incubating
public interface ProjectionDefinitionContext {

}
