/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;


import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A step in a "multi-step" composite aggregation definition
 * where one can define inner aggregations to get values from.
 */
@Incubating
public interface CompositeAggregationInnerStep {

	/**
	 * Defines one inner aggregation to get values from,
	 * based on a previously-built {@link SearchAggregation}.
	 *
	 * @param aggregation The inner aggregation.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the inner aggregation.
	 */
	<V1> CompositeAggregationFrom1AsStep<V1> from(SearchAggregation<V1> aggregation);

	/**
	 * Defines one inner aggregation to get values from,
	 * based on an almost-built {@link SearchAggregation}.
	 *
	 * @param dslFinalStep A final step in the aggregation DSL allowing the retrieval of the inner aggregation.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the inner aggregation.
	 */
	default <V1> CompositeAggregationFrom1AsStep<V1> from(AggregationFinalStep<V1> dslFinalStep) {
		return from( dslFinalStep.toAggregation() );
	}

	/**
	 * Defines two inner aggregations to get values from,
	 * based on previously-built {@link SearchAggregation}s.
	 *
	 * @param aggregation1 The first inner aggregation.
	 * @param aggregation2 The second inner aggregation.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner aggregation.
	 * @param <V2> The type of values returned by the second inner aggregation.
	 */
	<V1, V2> CompositeAggregationFrom2AsStep<V1, V2> from(SearchAggregation<V1> aggregation1,
			SearchAggregation<V2> aggregation2);

	/**
	 * Defines two inner aggregations to get values from,
	 * based on almost-built {@link SearchAggregation}s.
	 *
	 * @param dslFinalStep1 A final step in the aggregation DSL allowing the retrieval of the first inner aggregation.
	 * @param dslFinalStep2 A final step in the aggregation DSL allowing the retrieval of the second inner aggregation.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner aggregation.
	 * @param <V2> The type of values returned by the second inner aggregation.
	 */
	default <V1, V2> CompositeAggregationFrom2AsStep<V1, V2> from(AggregationFinalStep<V1> dslFinalStep1,
			AggregationFinalStep<V2> dslFinalStep2) {
		return from( dslFinalStep1.toAggregation(), dslFinalStep2.toAggregation() );
	}

	/**
	 * Defines three inner aggregations to get values from,
	 * based on previously-built {@link SearchAggregation}s.
	 *
	 * @param aggregation1 The first inner aggregation.
	 * @param aggregation2 The second inner aggregation.
	 * @param aggregation3 The third inner aggregation.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner aggregation.
	 * @param <V2> The type of values returned by the second inner aggregation.
	 * @param <V3> The type of values returned by the third inner aggregation.
	 */
	<V1, V2, V3> CompositeAggregationFrom3AsStep<V1, V2, V3> from(SearchAggregation<V1> aggregation1,
			SearchAggregation<V2> aggregation2, SearchAggregation<V3> aggregation3);

	/**
	 * Defines three inner aggregations to get values from,
	 * based on almost-built {@link SearchAggregation}s.
	 *
	 * @param dslFinalStep1 A final step in the aggregation DSL allowing the retrieval of the first inner aggregation.
	 * @param dslFinalStep2 A final step in the aggregation DSL allowing the retrieval of the second inner aggregation.
	 * @param dslFinalStep3 A final step in the aggregation DSL allowing the retrieval of the third inner aggregation.
	 * @return The next DSL step.
	 * @param <V1> The type of values returned by the first inner aggregation.
	 * @param <V2> The type of values returned by the second inner aggregation.
	 * @param <V3> The type of values returned by the third inner aggregation.
	 */
	default <V1, V2, V3> CompositeAggregationFrom3AsStep<V1, V2, V3> from(AggregationFinalStep<V1> dslFinalStep1,
			AggregationFinalStep<V2> dslFinalStep2, AggregationFinalStep<V3> dslFinalStep3) {
		return from( dslFinalStep1.toAggregation(), dslFinalStep2.toAggregation(), dslFinalStep3.toAggregation() );
	}

	/**
	 * Defines multiple inner aggregations to get values from,
	 * based on previously-built {@link SearchAggregation}s.
	 *
	 * @param aggregations The inner aggregations, in order.
	 * @return The next DSL step.
	 */
	CompositeAggregationFromAsStep from(SearchAggregation<?>... aggregations);

	/**
	 * Defines multiple inner aggregations to get values from,
	 * based on almost-built {@link SearchAggregation}s.
	 *
	 * @param dslFinalSteps The final steps in the aggregation DSL allowing the retrieval of inner aggregations, in order.
	 * @return The next DSL step.
	 */
	CompositeAggregationFromAsStep from(AggregationFinalStep<?>... dslFinalSteps);

}
