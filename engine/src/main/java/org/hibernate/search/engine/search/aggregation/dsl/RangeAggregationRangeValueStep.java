/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where the aggregation value for the range can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link RangeAggregationOptionsStep#filter(Function)}.
 * @param <F> The type of the targeted field.
 */
public interface RangeAggregationRangeValueStep<
		SR,
		PDF extends TypedSearchPredicateFactory<SR>,
		F> {

	<T> RangeAggregationOptionsStep<SR, ?, PDF, F, Map<Range<F>, T>> value(SearchAggregation<T> aggregation);

	default <T> RangeAggregationOptionsStep<SR, ?, PDF, F, Map<Range<F>, T>> value(AggregationFinalStep<T> aggregation) {
		return value( aggregation.toAggregation() );
	}
}
