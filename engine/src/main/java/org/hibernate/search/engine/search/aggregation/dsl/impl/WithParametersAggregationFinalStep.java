/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;
import org.hibernate.search.engine.search.common.NamedValues;

public class WithParametersAggregationFinalStep<A> implements AggregationFinalStep<A> {

	private final WithParametersAggregationBuilder<A> builder;

	public WithParametersAggregationFinalStep(
			SearchAggregationDslContext<?, ?> dslContext,
			Function<? super NamedValues, ? extends AggregationFinalStep<A>> aggregationCreator) {
		builder = dslContext.scope().aggregationBuilders().withParameters();
		builder.creator( aggregationCreator );
	}

	@Override
	public SearchAggregation<A> toAggregation() {
		return builder.build();
	}
}
