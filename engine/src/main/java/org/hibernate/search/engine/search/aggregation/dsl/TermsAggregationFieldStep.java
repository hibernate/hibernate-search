/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.aggregation.TermsAggregationFieldReference;

/**
 * The initial step in a "terms" aggregation definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
public interface TermsAggregationFieldStep<SR, PDF extends SearchPredicateFactory<SR>> {

	/**
	 * Target the given field in the terms aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @return The next step.
	 */
	default <F> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, Long>> field(String fieldPath, Class<F> type) {
		return field( fieldPath, type, ValueModel.MAPPING );
	}

	/**
	 * Target the given field in the terms aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @param convert Controls how the term values fetched from the backend should be converted.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert}.
	 * @return The next step.
	 * @deprecated Use {@link #field(String, Class, ValueModel)} instead.
	 */
	@Deprecated(since = "7.2")
	default <F> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, Long>> field(String fieldPath, Class<F> type,
			org.hibernate.search.engine.search.common.ValueConvert convert) {
		return field( fieldPath, type,
				org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * Target the given field in the terms aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @param valueModel The model of terms values, used to determine how term values fetched from the backend should be converted.
	 * See {@link ValueModel}.
	 * @return The next step.
	 */
	<F> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, Long>> field(String fieldPath, Class<F> type,
			ValueModel valueModel);

	/**
	 * Target the given field in the terms aggregation.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @param <F> The type of field values.
	 * @return The next step.
	 */
	default <F> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, Long>> field(
			TermsAggregationFieldReference<? super SR, F> fieldReference) {
		return field( fieldReference.absolutePath(), fieldReference.aggregationType(), fieldReference.valueModel() );
	}

}
