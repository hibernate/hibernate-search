/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationFromAsStep;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;

class CompositeAggregationFromAnyNumberAsStep extends AbstractCompositeAggregationFromAsStep
		implements CompositeAggregationFromAsStep {

	final SearchAggregation<?>[] inner;

	public CompositeAggregationFromAnyNumberAsStep(CompositeAggregationBuilder<?> builder, SearchAggregation<?>[] inner) {
		super( builder );
		this.inner = inner;
	}

	@Override
	SearchAggregation<?>[] toAggregationArray() {
		return inner;
	}

}
