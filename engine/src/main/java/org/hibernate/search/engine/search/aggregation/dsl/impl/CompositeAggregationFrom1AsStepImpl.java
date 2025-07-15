/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom1AsStep;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

class CompositeAggregationFrom1AsStepImpl<V1> extends AbstractCompositeAggregationFromAsStep
		implements CompositeAggregationFrom1AsStep<V1> {

	final SearchAggregation<V1> inner1;

	public CompositeAggregationFrom1AsStepImpl(CompositeAggregationBuilder<?> builder, SearchAggregation<V1> inner1) {
		super( builder );
		this.inner1 = inner1;
	}

	@Override
	SearchAggregation<?>[] toAggregationArray() {
		return new SearchAggregation<?>[] { inner1 };
	}

	@Override
	public <V> AggregationFinalStep<V> as(Function<V1, V> transformer) {
		return new CompositeAggregationFinalStep<>( ResultsCompositor.from( transformer ) );
	}
}
