/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

/**
 * The final step in a "range" aggregation definition, where optional parameters can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 * @param <F> The type of the targeted field.
 * @param <A> The type of result for this aggregation.
 */
public interface RangeAggregationOptionsStep<
		SR,
		S extends RangeAggregationOptionsStep<SR, ?, PDF, F, A>,
		PDF extends TypedSearchPredicateFactory<SR>,
		F,
		A>
		extends AggregationFinalStep<A>, AggregationFilterStep<SR, S, PDF> {

}
