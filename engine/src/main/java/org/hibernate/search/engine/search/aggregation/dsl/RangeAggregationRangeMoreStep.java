/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where optional parameters can be set,
 * (see the superinterface {@link RangeAggregationOptionsStep}),
 * or more ranges can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 * @param <N> The type of the next step.
 * @param <F> The type of the targeted field.
 */
public interface RangeAggregationRangeMoreStep<
		S extends RangeAggregationRangeMoreStep<?, ?, PDF, F>,
		N extends RangeAggregationOptionsStep<?, PDF, F, Map<Range<F>, Long>>,
		PDF extends SearchPredicateFactory,
		F>
		extends RangeAggregationOptionsStep<N, PDF, F, Map<Range<F>, Long>>,
		RangeAggregationRangeStep<S, PDF, F> {

}
