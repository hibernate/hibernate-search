/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

/**
 * A base interface for subtypes of {@link TypedSearchAggregationFactory} allowing to
 * easily override the self type and predicate factory type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <SR> Scope root type.
 * @param <S> The self type, i.e. the exposed type of this factory.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
public interface ExtendedSearchAggregationFactory<
		SR,
		S extends ExtendedSearchAggregationFactory<SR, ?, PDF>,
		PDF extends TypedSearchPredicateFactory<SR>>
		extends TypedSearchAggregationFactory<SR> {

	@Override
	S withRoot(String objectFieldPath);

	@Override
	RangeAggregationFieldStep<SR, PDF> range();

	@Override
	TermsAggregationFieldStep<SR, PDF> terms();

	@Override
	SumAggregationFieldStep<SR, PDF> sum();

	@Override
	MinAggregationFieldStep<SR, PDF> min();

	@Override
	MaxAggregationFieldStep<SR, PDF> max();

	@SuppressWarnings("removal")
	@Deprecated(since = "8.1", forRemoval = true)
	@Override
	default CountValuesAggregationFieldStep<SR, PDF> count() {
		return countValues();
	}

	@Override
	CountValuesAggregationFieldStep<SR, PDF> countValues();

	@SuppressWarnings("removal")
	@Deprecated(since = "8.1", forRemoval = true)
	@Override
	default CountDistinctValuesAggregationFieldStep<SR, PDF> countDistinct() {
		return countDistinctValues();
	}

	@Override
	CountDistinctValuesAggregationFieldStep<SR, PDF> countDistinctValues();

	@Override
	AvgAggregationFieldStep<SR, PDF> avg();

}
