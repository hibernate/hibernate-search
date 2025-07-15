/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFrom3AsStep;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;
import org.hibernate.search.util.common.function.TriFunction;

class CompositeAggregationFrom3AsStepImpl<V1, V2, V3>
		extends AbstractCompositeAggregationFromAsStep
		implements CompositeAggregationFrom3AsStep<V1, V2, V3> {

	final SearchAggregation<V1> inner1;
	final SearchAggregation<V2> inner2;
	final SearchAggregation<V3> inner3;

	public CompositeAggregationFrom3AsStepImpl(CompositeAggregationBuilder<?> builder, SearchAggregation<V1> inner1,
			SearchAggregation<V2> inner2, SearchAggregation<V3> inner3) {
		super( builder );
		this.inner1 = inner1;
		this.inner2 = inner2;
		this.inner3 = inner3;
	}

	@Override
	public <V> AggregationFinalStep<V> as(TriFunction<V1, V2, V3, V> transformer) {
		return new CompositeAggregationFinalStep<>( ResultsCompositor.from( transformer ) );
	}

	@Override
	SearchAggregation<?>[] toAggregationArray() {
		return new SearchAggregation<?>[] { inner1, inner2, inner3 };
	}

}
