/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.util.common.function.TriFunction;

/**
 * The step in a "multi-step" composite projection definition
 * where 3 inner projections have been defined
 * and the result of the composite projection can be defined.
 *
 * @param <V1> The type of values returned by the first inner projection.
 * @param <V2> The type of values returned by the second inner projection.
 * @param <V3> The type of values returned by the third inner projection.
 */
public interface CompositeProjectionFrom3AsStep<V1, V2, V3>
		extends CompositeProjectionFromAsStep {

	/**
	 * Defines the result of the composite projection
	 * as the result of applying the given function to the three inner projections defined so far.
	 *
	 * @param transformer A function to transform the values of inner projections defined so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> CompositeProjectionValueStep<?, V> as(TriFunction<V1, V2, V3, V> transformer);

}
