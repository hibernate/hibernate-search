/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "multi-step" composite aggregation definition
 * where one or more inner aggregations have been defined
 * and the result of the composite aggregation can be defined.
 */
@Incubating
public interface CompositeAggregationFromAsStep {

	/**
	 * Defines the result of the composite aggregation
	 * as a {@link List} that will contain the results of inner aggregations defined so far, in order.
	 *
	 * @return The next DSL step.
	 */
	AggregationFinalStep<List<?>> asList();

	/**
	 * Defines the result of the composite aggregation
	 * as the result of applying the given function to a {@link List} containing
	 * the results of inner aggregations defined so far, in order.
	 *
	 * @param transformer A function to transform the values of inner aggregations added so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> AggregationFinalStep<V> asList(Function<? super List<?>, ? extends V> transformer);

	/**
	 * Defines the result of the composite aggregation
	 * as an object array that will contain the results of inner aggregations defined so far, in order.
	 *
	 * @return The next DSL step.
	 */
	AggregationFinalStep<Object[]> asArray();

	/**
	 * Defines the result of the composite aggregation
	 * as the result of applying the given function to an object array containing
	 * the results of inner aggregations defined so far, in order.
	 *
	 * @param transformer A function to transform the values of inner aggregations added so far.
	 * @return The next DSL step.
	 * @param <V> The type of values returned by the transformer.
	 */
	<V> AggregationFinalStep<V> asArray(Function<? super Object[], ? extends V> transformer);

}
