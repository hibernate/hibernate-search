/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;

/**
 * The step in a composite projection definition
 * where the projection (optionally) can be marked as multi-valued (returning Lists),
 * and where optional parameters can be set.
 * <p>
 * By default (if {@link #multi()} is not called), the projection is single-valued.
 *
 * @param <N> The next step if a method other than {@link #multi()} is called,
 * i.e. the return type of methods defined in {@link CompositeProjectionOptionsStep}
 * when called directly on this object.
 * @param <T> The type of composed projections.
 */
public interface CompositeProjectionValueStep<N extends CompositeProjectionOptionsStep<?, T>, T>
		extends CompositeProjectionOptionsStep<N, T> {

	/**
	 * Defines the projection as multi-valued, i.e. returning {@code List<T>} instead of {@code T}.
	 * <p>
	 * Calling {@link #multi()} is mandatory for {@link SearchProjectionFactory#object(String) object projections}
	 * on multi-valued object fields,
	 * otherwise the projection will throw an exception upon creating the search query.
	 * <p>
	 * Calling {@link #multi()} on {@link SearchProjectionFactory#composite() basic composite projections}
	 * is generally not useful: the only effect is that projected values will be wrapped in a one-element {@link List}.
	 *
	 * @return A new step to define optional parameters for the projection.
	 */
	CompositeProjectionOptionsStep<?, List<T>> multi();

}
