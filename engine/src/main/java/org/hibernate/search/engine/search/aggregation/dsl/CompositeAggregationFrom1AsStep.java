/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.util.common.annotation.Incubating;


/**
 * The step in a "multi-step" composite aggregation definition
 * where a single inner aggregation has been defined
 * and the result of the composite aggregation can be defined.
 *
 * @param <V1> The type of values returned by the single inner aggregation.
 */
@Incubating
public interface CompositeAggregationFrom1AsStep<V1>
		extends CompositeAggregationFromAsStep {

	/**
	 * Defines the result of the composite aggregation
	 * as the result of applying the given function to the single inner aggregation defined so far.
	 *
	 * @param transformer A function to transform the values of inner aggregations defined so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> AggregationFinalStep<V> as(Function<V1, V> transformer);

}
