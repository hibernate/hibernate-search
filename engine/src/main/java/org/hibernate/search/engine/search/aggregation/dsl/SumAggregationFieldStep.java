/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.reference.aggregation.SumAggregationFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial step in a "sum" aggregation definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
@Incubating
public interface SumAggregationFieldStep<SR, PDF extends TypedSearchPredicateFactory<SR>> {

	/**
	 * Target the given field in the sum aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @return The next step.
	 */
	default <F> SumAggregationOptionsStep<SR, ?, PDF, F> field(String fieldPath, Class<F> type) {
		return field( fieldPath, type, ValueModel.MAPPING );
	}

	/**
	 * Target the given field in the sum aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @param valueModel The model of terms values, used to determine how term values fetched from the backend should be converted.
	 * See {@link ValueModel}.
	 * @return The next step.
	 */
	<F> SumAggregationOptionsStep<SR, ?, PDF, F> field(String fieldPath, Class<F> type, ValueModel valueModel);

	/**
	 * Target the given field in the sum aggregation.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchAggregationFactory.html#field-references">definition</a> of the index field to aggregate.
	 * @param <F> The type of field values.
	 * @return The next step.
	 */
	@Incubating
	default <F> SumAggregationOptionsStep<SR, ?, PDF, F> field(SumAggregationFieldReference<SR, F> fieldReference) {
		return field( fieldReference.absolutePath(), fieldReference.aggregationType(), fieldReference.valueModel() );
	}
}
