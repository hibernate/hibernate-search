/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The final step in a "count distinct" aggregation definition, where optional parameters can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 */
@Incubating
public interface CountDistinctAggregationOptionsStep<
		SR,
		S extends CountDistinctAggregationOptionsStep<SR, ?, PDF>,
		PDF extends SearchPredicateFactory<SR>>
		extends AggregationFinalStep<Long>, AggregationFilterStep<SR, S, PDF> {

}
