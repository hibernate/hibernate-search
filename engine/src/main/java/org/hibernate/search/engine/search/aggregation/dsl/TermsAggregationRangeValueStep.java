/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

/**
 * The step in a "terms" aggregation definition where the aggregation value for the term can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link TermsAggregationOptionsStep#filter(Function)}.
 * @param <F> The type of the targeted field.
 */
public interface TermsAggregationRangeValueStep<
		SR,
		S extends TermsAggregationOptionsStep<SR, ?, PDF, F, A>,
		PDF extends TypedSearchPredicateFactory<SR>,
		F,
		A> extends TermsAggregationOptionsStep<SR, S, PDF, F, A> {

	<T> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, T>> value(SearchAggregation<T> aggregation);

	default <T> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, T>> value(AggregationFinalStep<T> aggregation) {
		return value( aggregation.toAggregation() );
	}
}
