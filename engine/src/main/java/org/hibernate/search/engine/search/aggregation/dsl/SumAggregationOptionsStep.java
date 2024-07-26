/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The final step in a "sum" aggregation definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 * @param <F> The type of the targeted field. The type of result for this aggregation.
 */
@Incubating
public interface SumAggregationOptionsStep<
		S extends SumAggregationOptionsStep<?, PDF, F>,
		PDF extends SearchPredicateFactory,
		F>
		extends AggregationFinalStep<F>, AggregationFilterStep<S, PDF> {

}
