/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl;


import java.util.Collection;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.query.SearchFetchable;
import org.hibernate.search.engine.search.query.SearchResult;

/**
 * The final step in a query definition, where optional parameters such as {@link #sort(Function) sorts} can be set,
 * and where the query can be {@link SearchFetchable executed} or {@link #toQuery() retrieved as an object}.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * May be a subtype of SearchQueryOptionsStep with more exposed methods.
 * @param <H> The type of hits for the created query.
 * @param <SF> The type of factory used to create sorts in {@link #sort(Function)}.
 * @param <AF> The type of factory used to create aggregations in {@link #aggregation(AggregationKey, Function)}.
 */
public interface SearchQueryOptionsStep<
				S extends SearchQueryOptionsStep<? extends S, H, SF, AF>,
				H,
				SF extends SearchSortFactory,
				AF extends SearchAggregationFactory
		>
		extends SearchQueryFinalStep<H>, SearchFetchable<H> {

	/**
	 * Configure routing of the search query.
	 * <p>
	 * Useful when indexes are sharded, to limit the number of shards interrogated by the search query.
	 * <p>
	 * This method may be called multiple times,
	 * in which case all submitted routing keys will be taken into account.
	 * <p>
	 * By default, if routing is not configured, all shards will be queried.
	 *
	 * @param routingKey A string key. All shards matching this key will be queried.
	 * @return {@code this}, for method chaining.
	 */
	S routing(String routingKey);

	/**
	 * Configure routing of the search query.
	 * <p>
	 * Similar to {@link #routing(String)}, but allows passing multiple keys in a single call.
	 *
	 * @param routingKeys A collection containing zero, one or multiple string keys.
	 * @return {@code this}, for method chaining.
	 */
	S routing(Collection<String> routingKeys);

	/**
	 * Add a sort to this query.
	 * @param sort A {@link SearchSort} object obtained from the search scope.
	 * @return {@code this}, for method chaining.
	 */
	S sort(SearchSort sort);

	/**
	 * Add a sort to this query.
	 * @param sortContributor A function that will use the factory passed in parameter to create a sort,
	 * returning the final step in the sort DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S sort(Function<? super SF, ? extends SortFinalStep> sortContributor);

	/**
	 * Add an aggregation to this query.
	 * @param key The key that will be used to {@link SearchResult#getAggregation(AggregationKey) retrieve the aggregation}
	 * from the {@link SearchResult}.
	 * @param aggregation A {@link SearchAggregation} object obtained from the search scope.
	 * @param <T> The type of aggregation values.
	 * @return {@code this}, for method chaining.
	 */
	<T> S aggregation(AggregationKey<T> key, SearchAggregation<T> aggregation);

	/**
	 * Add an aggregation to this query.
	 * @param key The key that will be used to {@link SearchResult#getAggregation(AggregationKey) retrieve the aggregation}
	 * from the {@link SearchResult}.
	 * @param aggregationContributor A function that will use the factory passed in parameter to create an aggregation,
	 * returning the final step in the sort DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of aggregation values.
	 * @return {@code this}, for method chaining.
	 */
	<T> S aggregation(AggregationKey<T> key, Function<? super AF, ? extends AggregationFinalStep<T>> aggregationContributor);

}
