/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.reference.aggregation.CountAggregationFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial step in a "count" aggregation definition, where the kind of the aggregation can be picked.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
@Incubating
public interface CountAggregationKindStep<SR, PDF extends TypedSearchPredicateFactory<SR>> {

	/**
	 * Count documents that match the query.
	 * <p>
	 * This aggregation may be useful for building {@link SearchAggregationFactory#range()} or {@link SearchAggregationFactory#terms()} aggregations.
	 *
	 * @return The next step.
	 */
	@Incubating
	CountDocumentsAggregationFinalStep documents();

	/**
	 * Count the number of non-empty values for the given field.
	 * <p>
	 * For a multi-valued field, the resulting count
	 * may be greater than the number of matched documents.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @return The next step.
	 */
	@Incubating
	CountValuesAggregationOptionsStep<SR, ?, PDF> field(String fieldPath);

	/**
	 * Count the number of non-empty values for the given field.
	 * <p>
	 * For a multi-valued field, the resulting count
	 * may be greater than the number of matched documents.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchAggregationFactory.html#field-references">definition</a> of the index field to aggregate.
	 * @return The next step.
	 */
	@Incubating
	default CountValuesAggregationOptionsStep<SR, ?, PDF> field(CountAggregationFieldReference<SR> fieldReference) {
		return field( fieldReference.absolutePath() );
	}
}
