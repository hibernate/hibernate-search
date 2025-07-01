/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctValuesAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctValuesAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public class CountDistinctValuesAggregationFieldStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>>
		implements CountDistinctValuesAggregationFieldStep<SR, PDF> {
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	public CountDistinctValuesAggregationFieldStepImpl(SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CountDistinctValuesAggregationOptionsStep<SR, ?, PDF> field(String fieldPath) {
		SearchFilterableAggregationBuilder<Long> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.COUNT_DISTINCT_VALUES );
		return new CountDistinctValuesAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
