/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.aggregation.spi;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;

/**
 * Represents the current context in the search DSL,
 * including in particular the aggregation builder factory.
 *
 * @param <F> The type of aggregation factory.
 */
public interface SearchAggregationDslContext<F extends SearchAggregationBuilderFactory> {

	/**
	 * @return The aggregation builder factory. Will always return the exact same instance.
	 */
	F getBuilderFactory();

}
