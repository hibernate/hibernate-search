/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.List;
import java.util.function.Function;

/**
 * The step in a "multi-step" composite projection definition
 * where one or more inner projections have been defined
 * and the result of the composite projection can be defined.
 */
public interface CompositeProjectionFromAsStep {

	/**
	 * Defines the result of the composite projection
	 * as a {@link List} that will contain the results of inner projections defined so far, in order.
	 *
	 * @return The next DSL step.
	 */
	CompositeProjectionValueStep<?, List<?>> asList();

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to a {@link List} containing
	 * the results of inner projections defined so far, in order.
	 *
	 * @param transformer A function to transform the values of inner projections added so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionValueStep<?, V> asList(Function<? super List<?>, ? extends V> transformer);

	/**
	 * Defines the result of the composite projection
	 * as an object array that will contain the results of inner projections defined so far, in order.
	 *
	 * @return The next DSL step.
	 */
	CompositeProjectionValueStep<?, Object[]> asArray();

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to an object array containing
	 * the results of inner projections defined so far, in order.
	 *
	 * @param transformer A function to transform the values of inner projections added so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionValueStep<?, V> asArray(Function<? super Object[], ? extends V> transformer);

}
