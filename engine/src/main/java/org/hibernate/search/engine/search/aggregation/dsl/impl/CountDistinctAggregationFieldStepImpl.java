/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public class CountDistinctAggregationFieldStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>>
		implements CountDistinctAggregationFieldStep<SR, PDF> {
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	public CountDistinctAggregationFieldStepImpl(SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CountDistinctAggregationOptionsStep<SR, ?, PDF> field(String fieldPath) {
		SearchFilterableAggregationBuilder<Long> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.COUNT_DISTINCT );
		return new CountDistinctAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
