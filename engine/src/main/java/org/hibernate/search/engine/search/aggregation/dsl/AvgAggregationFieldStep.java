/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial step in an "avg" aggregation definition, where the target field can be set.
 *
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
@Incubating
public interface AvgAggregationFieldStep<PDF extends SearchPredicateFactory> {

	/**
	 * Target the given field in the avg aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values or {@link Double} if a double result is required.
	 * @return The next step.
	 */
	default <F> AvgAggregationOptionsStep<?, PDF, F> field(String fieldPath, Class<F> type) {
		return field( fieldPath, type, ValueModel.MAPPING );
	}

	/**
	 * Target the given field in the avg aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values or {@link Double} if a double result is required.
	 * @param valueModel The model of aggregation values, used to determine how computed aggregation value should be converted.
	 * See {@link ValueModel}.
	 * @return The next step.
	 */
	<F> AvgAggregationOptionsStep<?, PDF, F> field(String fieldPath, Class<F> type,
			ValueModel valueModel);

}
