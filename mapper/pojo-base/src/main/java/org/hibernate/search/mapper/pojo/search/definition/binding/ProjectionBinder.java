/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A component able to define a projection using the Hibernate Search Projection DSL.
 * <p>
 * This definition takes advantage of provided metadata
 * to pick, configure and create a {@link SearchProjection}.
 * <p>
 * Used in particular for projections defined using mapper features, e.g. Java annotations
 * (see {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection},
 * {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection}).
 *
 * @see SearchProjection
 * @see org.hibernate.search.engine.search.projection.definition.ProjectionDefinition
 */
@Incubating
public interface ProjectionBinder {

	/**
	 * Binds a constructor parameter to a projection.
	 * <p>
	 * The context passed in parameter provides various information about the constructor parameter being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code definition*(...)} methods on the context
	 * to set the projection.
	 *
	 * @param context A context object providing information about the constructor parameter being bound,
	 * and expecting a call to one of its {@code definition*(...)} methods.
	 */
	void bind(ProjectionBindingContext context);

}
