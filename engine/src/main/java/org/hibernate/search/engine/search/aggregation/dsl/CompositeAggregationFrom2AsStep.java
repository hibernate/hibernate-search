/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.BiFunction;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "multi-step" composite aggregation definition
 * where 2 inner aggregations have been defined
 * and the result of the composite aggregation can be defined.
 *
 * @param <V1> The type of values returned by the first inner aggregation.
 * @param <V2> The type of values returned by the second inner aggregation.
 */
@Incubating
public interface CompositeAggregationFrom2AsStep<V1, V2>
		extends CompositeAggregationFromAsStep {

	/**
	 * Defines the result of the composite aggregation
	 * as the result of applying the given function to the two inner aggregations defined so far.
	 *
	 * @param transformer A function to transform the values of inner aggregations defined so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> AggregationFinalStep<V> as(BiFunction<V1, V2, V> transformer);

}
