/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "terms" aggregation definition where the aggregation value for the term can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link TermsAggregationOptionsStep#filter(Function)}.
 * @param <F> The type of the targeted field.
 */
@Incubating
public interface TermsAggregationValueStep<
		SR,
		S extends TermsAggregationOptionsStep<SR, ?, PDF, F, A>,
		PDF extends TypedSearchPredicateFactory<SR>,
		F,
		A> extends TermsAggregationOptionsStep<SR, S, PDF, F, A> {

	/**
	 * Specify which aggregation to apply to the documents with same terms.
	 * <p>
	 * This allows to "group" the documents by "terms" and then apply one of the aggregations from {@link SearchAggregationFactory}
	 * to the documents in that group.
	 *
	 * @param aggregation The aggregation to apply to the documents for each term.
	 * @return The next step in terms aggregation definition.
	 * @param <T> The type of the aggregated results for a term.
	 */
	@Incubating
	<T> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, T>> value(SearchAggregation<T> aggregation);

	/**
	 * Specify which aggregation to apply to the documents with same terms.
	 * <p>
	 * This allows to "group" the documents by "terms" and then apply one of the aggregations from {@link SearchAggregationFactory}
	 * to the documents in that group.
	 *
	 * @param aggregation The aggregation to apply to the documents for each term.
	 * @return The next step in terms aggregation definition.
	 * @param <T> The type of the aggregated results for a term.
	 */
	@Incubating
	default <T> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, T>> value(AggregationFinalStep<T> aggregation) {
		return value( aggregation.toAggregation() );
	}
}
