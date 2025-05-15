/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The final step in a "min" aggregation definition, where optional parameters can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 * @param <F> The type of the targeted field. The type of result for this aggregation.
 */
@Incubating
public interface MinAggregationOptionsStep<
		SR,
		S extends MinAggregationOptionsStep<SR, ?, PDF, F>,
		PDF extends TypedSearchPredicateFactory<SR>,
		F>
		extends AggregationFinalStep<F>, AggregationFilterStep<SR, S, PDF> {

}
