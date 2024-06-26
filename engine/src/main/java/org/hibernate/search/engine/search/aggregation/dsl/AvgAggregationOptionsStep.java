/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The final step in a "avg" aggregation definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 */
public interface AvgAggregationOptionsStep<
		S extends AvgAggregationOptionsStep<?, PDF>,
		PDF extends SearchPredicateFactory>
		extends AggregationFinalStep<Double>, AggregationFilterStep<S, PDF> {

}
