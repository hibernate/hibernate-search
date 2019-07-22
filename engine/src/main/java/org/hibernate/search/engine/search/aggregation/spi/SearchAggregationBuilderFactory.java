/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * A factory for search aggregation builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search aggregations.
 *
 * @param <C> The type of query element collector
 */
public interface SearchAggregationBuilderFactory<C> {

	/**
	 * Contribute an aggregation to a collector.
	 * <p>
	 * May be called multiple times per collector, if there are multiple aggregations.
	 *
	 * @param collector The query element collector.
	 * @param key The aggregation key, used to later retrieve the result of the aggregation.
	 * @param aggregation The aggregation implementation.
	 * @param <A> The type of result for this aggregation.
	 */
	<A> void contribute(C collector, AggregationKey<A> key, SearchAggregation<A> aggregation);

	<T> TermsAggregationBuilder<T> createTermsAggregationBuilder(String absoluteFieldPath,
			Class<T> expectedType, ValueConvert convert);

	<T> RangeAggregationBuilder<T> createRangeAggregationBuilder(String absoluteFieldPath,
			Class<T> expectedType, ValueConvert convert);

}
