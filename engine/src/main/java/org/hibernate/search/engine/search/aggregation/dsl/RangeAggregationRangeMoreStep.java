/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;

/**
 * The step in a "range" aggregation definition where optional parameters can be set,
 * (see the superinterface {@link RangeAggregationOptionsStep}),
 * or more ranges can be added.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 * @param <N> The type of the next step.
 * @param <F> The type of the targeted field.
 */
public interface RangeAggregationRangeMoreStep<
		SR,
		S extends RangeAggregationRangeMoreStep<SR, ?, ?, PDF, F>,
		N extends RangeAggregationOptionsStep<SR, ?, PDF, F, Map<Range<F>, Long>>,
		PDF extends TypedSearchPredicateFactory<SR>,
		F>
		extends RangeAggregationOptionsStep<SR, N, PDF, F, Map<Range<F>, Long>>,
		RangeAggregationRangeStep<SR, S, PDF, F> {

}
