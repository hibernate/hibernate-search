/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where the aggregation value for the range can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link RangeAggregationOptionsStep#filter(Function)}.
 * @param <F> The type of the targeted field.
 */
@Incubating
public interface RangeAggregationRangeValueStep<
		SR,
		PDF extends TypedSearchPredicateFactory<SR>,
		F> {
	/**
	 * Specify which aggregation to apply to the documents within the range.
	 * <p>
	 * This allows to "group" the documents by "ranges" and then apply one of the aggregations from {@link SearchAggregationFactory}
	 * to the documents in that group.
	 *
	 * @param aggregation The aggregation to apply to the documents within each range.
	 * @return The next step in range aggregation definition.
	 * @param <T> The type of the aggregated results within a range.
	 */
	@Incubating
	<T> RangeAggregationOptionsStep<SR, ?, PDF, F, Map<Range<F>, T>> value(SearchAggregation<T> aggregation);

	/**
	 * Specify which aggregation to apply to the documents within the range.
	 * <p>
	 * This allows to "group" the documents by "ranges" and then apply one of the aggregations from {@link SearchAggregationFactory}
	 * to the documents in that group.
	 *
	 * @param aggregation The aggregation to apply to the documents within each range.
	 * @return The next step in range aggregation definition.
	 * @param <T> The type of the aggregated results within a range.
	 */
	@Incubating
	default <T> RangeAggregationOptionsStep<SR, ?, PDF, F, Map<Range<F>, T>> value(AggregationFinalStep<T> aggregation) {
		return value( aggregation.toAggregation() );
	}
}
