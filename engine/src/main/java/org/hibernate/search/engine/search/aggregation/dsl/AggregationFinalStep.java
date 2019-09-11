/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
