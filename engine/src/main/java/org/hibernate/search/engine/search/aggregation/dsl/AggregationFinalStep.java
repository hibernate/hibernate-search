/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;

/**
 * The final step in an aggregation definition, where the aggregation can be retrieved.
 *
 * @param <A> The type of result for this aggregation.
 */
public interface AggregationFinalStep<A> {

	/**
	 * Create a {@link SearchAggregation} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchAggregation} resulting from the previous DSL steps.
	 */
	SearchAggregation<A> toAggregation();

}
