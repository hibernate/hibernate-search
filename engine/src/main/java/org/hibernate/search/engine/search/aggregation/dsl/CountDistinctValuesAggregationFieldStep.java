/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.reference.aggregation.CountValuesAggregationFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial step in a "count distinct" aggregation definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <PDF> The type of factory used to create predicates in {@link AggregationFilterStep#filter(Function)}.
 */
@Incubating
public interface CountDistinctValuesAggregationFieldStep<SR, PDF extends TypedSearchPredicateFactory<SR>> {

	/**
	 * Target the given field in the count distinct values aggregation.
	 *
	 * @param fieldPath The <a href="SearchAggregationFactory.html#field-paths">path</a> to the index field to aggregate.
	 * @return The next step.
	 */
	CountDistinctValuesAggregationOptionsStep<SR, ?, PDF> field(String fieldPath);

	/**
	 * Target the given field in the count distinct values aggregation.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchAggregationFactory.html#field-references">definition</a> of the index field to aggregate.
	 * @return The next step.
	 */
	@Incubating
	default CountDistinctValuesAggregationOptionsStep<SR, ?, PDF> field(CountValuesAggregationFieldReference<SR> fieldReference) {
		return field( fieldReference.absolutePath() );
	}
}
