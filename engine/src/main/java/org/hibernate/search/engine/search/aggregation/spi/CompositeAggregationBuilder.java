/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

public interface CompositeAggregationBuilder<T> extends SearchAggregationBuilder<T> {

	CompositeAggregationBuilder<T> innerAggregations(SearchAggregation<?>[] inners);

	<V> CompositeAggregationBuilder<V> compositor(ResultsCompositor<?, V> compositor);

}
