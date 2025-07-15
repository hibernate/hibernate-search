/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.BiFunction;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom2AsStep;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

class CompositeAggregationFrom2AsStepImpl<V1, V2>
		extends AbstractCompositeAggregationFromAsStep
		implements CompositeAggregationFrom2AsStep<V1, V2> {

	final SearchAggregation<V1> inner1;
	final SearchAggregation<V2> inner2;

	public CompositeAggregationFrom2AsStepImpl(CompositeAggregationBuilder<?> builder, SearchAggregation<V1> inner1,
			SearchAggregation<V2> inner2) {
		super( builder );
		this.inner1 = inner1;
		this.inner2 = inner2;
	}

	@Override
	public <V> AggregationFinalStep<V> as(BiFunction<V1, V2, V> transformer) {
		return new CompositeAggregationFinalStep<>( ResultsCompositor.from( transformer ) );
	}

	@Override
	SearchAggregation<?>[] toAggregationArray() {
		return new SearchAggregation<?>[] { inner1, inner2 };
	}

}
