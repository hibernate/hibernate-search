/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.AvgAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.AvgAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.SearchFilterableAggregationBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public class AvgAggregationFieldStepImpl<PDF extends SearchPredicateFactory>
		implements AvgAggregationFieldStep<PDF> {
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	public AvgAggregationFieldStepImpl(SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public AvgAggregationOptionsStep<?, PDF> field(String fieldPath) {
		SearchFilterableAggregationBuilder<Double> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.AVG );
		return new AvgAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
