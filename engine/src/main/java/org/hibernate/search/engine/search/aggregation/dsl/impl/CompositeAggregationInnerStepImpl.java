/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom1AsStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom2AsStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom3AsStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFromAsStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationInnerStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;

public class CompositeAggregationInnerStepImpl implements CompositeAggregationInnerStep {

	private final CompositeAggregationBuilder<?> builder;

	public CompositeAggregationInnerStepImpl(SearchAggregationDslContext<?, ?, ?> dslContext) {
		this.builder = dslContext.scope().aggregationBuilders().compositeAggregation();
	}

	@Override
	public <V1> CompositeAggregationFrom1AsStep<V1> from(SearchAggregation<V1> aggregation) {
		return new CompositeAggregationFrom1AsStepImpl<>( builder, aggregation );
	}

	@Override
	public <V1, V2> CompositeAggregationFrom2AsStep<V1, V2> from(SearchAggregation<V1> aggregation1,
			SearchAggregation<V2> aggregation2) {
		return new CompositeAggregationFrom2AsStepImpl<>( builder, aggregation1, aggregation2 );
	}

	@Override
	public <V1, V2, V3> CompositeAggregationFrom3AsStep<V1, V2, V3> from(SearchAggregation<V1> aggregation1,
			SearchAggregation<V2> aggregation2, SearchAggregation<V3> aggregation3) {
		return new CompositeAggregationFrom3AsStepImpl<>( builder, aggregation1, aggregation2, aggregation3 );
	}

	@Override
	public CompositeAggregationFromAsStep from(SearchAggregation<?>... aggregations) {
		return new CompositeAggregationFromAnyNumberAsStep( builder, aggregations );
	}

	@Override
	public CompositeAggregationFromAsStep from(AggregationFinalStep<?>... dslFinalSteps) {
		SearchAggregation<?>[] aggregations = new SearchAggregation<?>[dslFinalSteps.length];
		for ( int i = 0; i < dslFinalSteps.length; i++ ) {
			aggregations[i] = dslFinalSteps[i].toAggregation();
		}
		return from( aggregations );
	}
}
