/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The final step in a "count" aggregation definition, where optional parameters can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 */
@Incubating
public interface CountValuesAggregationOptionsStep<
		SR,
		S extends CountValuesAggregationOptionsStep<SR, ?, PDF>,
		PDF extends TypedSearchPredicateFactory<SR>>
		extends AggregationFinalStep<Long>, AggregationFilterStep<SR, S, PDF> {

	/**
	 * Count only distinct field values.
	 *
	 * @return The next step.
	 */
	default S distinct() {
		return distinct( true );
	}

	/**
	 * Specify whether to count distinct or all field values.
	 * @param distinct Use {@code true} if only distinct field values should be counted, {@code false} otherwise.
	 * @return The next step.
	 */
	S distinct(boolean distinct);
}
