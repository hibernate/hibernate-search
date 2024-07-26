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
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public class CountAggregationFieldStepImpl<PDF extends SearchPredicateFactory>
		implements CountAggregationFieldStep<PDF> {
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	public CountAggregationFieldStepImpl(SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public CountAggregationOptionsStep<?, PDF> field(String fieldPath) {
		SearchFilterableAggregationBuilder<Long> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.COUNT );
		return new CountAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
