/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;

/**
 * A search aggregation builder, i.e. an object responsible for collecting parameters
 * and then building a search aggregation.
 *
 * @param <A> The type of resulting aggregations.
 */
public interface SearchAggregationBuilder<A> {

	SearchAggregation<A> build();
}
