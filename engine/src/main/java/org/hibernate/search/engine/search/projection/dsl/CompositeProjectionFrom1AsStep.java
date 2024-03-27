/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

/**
 * The step in a "multi-step" composite projection definition
 * where a single inner projection has been defined
 * and the result of the composite projection can be defined.
 *
 * @param <V1> The type of values returned by the single inner projection.
 */
public interface CompositeProjectionFrom1AsStep<V1>
		extends CompositeProjectionFromAsStep {

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to the single inner projection defined so far.
	 *
	 * @param transformer A function to transform the values of inner projections defined so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionValueStep<?, V> as(Function<V1, V> transformer);

}
