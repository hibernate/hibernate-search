/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.MinAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.MinAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.FieldMetricAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.impl.Contracts;

public class MinAggregationFieldStepImpl<PDF extends SearchPredicateFactory> implements MinAggregationFieldStep<PDF> {
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	public MinAggregationFieldStepImpl(SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public <F> MinAggregationOptionsStep<?, PDF, F> field(String fieldPath, Class<F> type, ValueModel valueModel) {
		Contracts.assertNotNull( fieldPath, "fieldPath" );
		Contracts.assertNotNull( type, "type" );
		FieldMetricAggregationBuilder<F> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.MIN ).type( type, valueModel );
		return new MinAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
