/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.CountAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public class CountAggregationFieldStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>>
		implements CountAggregationFieldStep<SR, PDF> {
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	public CountAggregationFieldStepImpl(SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CountAggregationOptionsStep<SR, ?, PDF> field(String fieldPath) {
		SearchFilterableAggregationBuilder<Long> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.COUNT );
		return new CountAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
