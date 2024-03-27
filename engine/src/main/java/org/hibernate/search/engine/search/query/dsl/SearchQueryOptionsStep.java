/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFinalStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.SearchFetchable;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The final step in a query definition, where optional parameters such as {@link #sort(Function) sorts} can be set,
 * and where the query can be {@link SearchFetchable executed} or {@link #toQuery() retrieved as an object}.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * May be a subtype of SearchQueryOptionsStep with more exposed methods.
 * @param <H> The type of hits for the created query.
 * @param <LOS> The type of the initial step of the loading options definition DSL accessible through {@link #loading(Consumer)}.
 * @param <SF> The type of factory used to create sorts in {@link #sort(Function)}.
 * @param <AF> The type of factory used to create aggregations in {@link #aggregation(AggregationKey, Function)}.
 */
public interface SearchQueryOptionsStep<
		S extends SearchQueryOptionsStep<?, H, LOS, SF, AF>,
		H,
		LOS,
		SF extends SearchSortFactory,
		AF extends SearchAggregationFactory>
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
	 * Stop the query and return truncated results after a given timeout.
	 * <p>
	 * The timeout is handled on a best effort basis:
	 * Hibernate Search will *try* to stop the query as soon as possible after the timeout.
	 *
	 * @param timeout Timeout value.
	 * @param timeUnit Timeout unit.
	 * @return {@code this}, for method chaining.
	 */
	S truncateAfter(long timeout, TimeUnit timeUnit);

	/**
	 * Stop the query and throw a {@link org.hibernate.search.util.common.SearchTimeoutException} after a given timeout.
	 * <p>
	 * The timeout is handled on a best effort basis:
	 * Hibernate Search will *try* to stop the query as soon as possible after the timeout.
	 * However, this method is more likely to trigger an early stop than {@link #truncateAfter(long, TimeUnit)}.
	 *
	 * @param timeout Timeout value.
	 * @param timeUnit Timeout unit.
	 * @return {@code this}, for method chaining.
	 */
	S failAfter(long timeout, TimeUnit timeUnit);

	/**
	 * Configure entity loading for this query.
	 * @param loadingOptionsContributor A consumer that will alter the loading options passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S loading(Consumer<? super LOS> loadingOptionsContributor);

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
	 * @param key The key that will be used to {@link SearchResult#aggregation(AggregationKey) retrieve the aggregation}
	 * from the {@link SearchResult}.
	 * @param aggregation A {@link SearchAggregation} object obtained from the search scope.
	 * @param <T> The type of aggregation values.
	 * @return {@code this}, for method chaining.
	 */
	<T> S aggregation(AggregationKey<T> key, SearchAggregation<T> aggregation);

	/**
	 * Add an aggregation to this query.
	 * @param key The key that will be used to {@link SearchResult#aggregation(AggregationKey) retrieve the aggregation}
	 * from the {@link SearchResult}.
	 * @param aggregationContributor A function that will use the factory passed in parameter to create an aggregation,
	 * returning the final step in the sort DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of aggregation values.
	 * @return {@code this}, for method chaining.
	 */
	<T> S aggregation(AggregationKey<T> key, Function<? super AF, ? extends AggregationFinalStep<T>> aggregationContributor);

	/**
	 * Allow Hibernate Search to return a lower-bound estimate of the total hit count
	 * if it exceeds {@code totalHitCountThreshold}.
	 * <p>
	 * Allowing Hibernate Search to return a lower-bound estimate of the total hit count can lead to significantly fewer
	 * index scans and yield significant performance improvements,
	 * in particular when sorting by score (the default) on a large result set.
	 * <p>
	 * Note this optimization has no effect when also requesting aggregations.
	 *
	 * @param totalHitCountThreshold the value below which the hit count is always exact
	 * @return {@code this}, for method chaining.
	 * @see SearchResultTotal
	 */
	S totalHitCountThreshold(long totalHitCountThreshold);

	/**
	 * Configure the default highlighter.
	 * <p>
	 * For specifying the fields to highlight see {@link SearchProjectionFactory#highlight(String)}.
	 * For providing field-specific settings to override the ones set here see {@link #highlighter(String, Function)}.
	 * <p>
	 * Backend specific defaults will be used if no configuration is provided.
	 *
	 * @param highlighterContributor a function that will use a highlighter factory to configure the default highlighter for this query.
	 *
	 * @return {@code this}, for method chaining.
	 */
	@Incubating
	S highlighter(Function<? super SearchHighlighterFactory, ? extends HighlighterFinalStep> highlighterContributor);

	/**
	 * Configure the default highlighter.
	 * <p>
	 * For specifying the fields to highlight see {@link SearchProjectionFactory#highlight(String)}.
	 * For providing field-specific settings to override the ones set here see {@link #highlighter(String, Function)}.
	 * <p>
	 * Backend specific defaults will be used if no configuration is provided.
	 *
	 * @param highlighter a highlighter obtained from the search scope to be used as the default highlighter for this query.
	 *
	 * @return {@code this}, for method chaining.
	 */
	@Incubating
	S highlighter(SearchHighlighter highlighter);

	/**
	 * Configure a named highlighter.
	 * <p>
	 * Named highlighters are to be referenced in {@link HighlightProjectionOptionsStep#highlighter(String) highlight projections}.
	 * <p>
	 * Named highlighters override the settings set by a default highlighter if such was configured for this query.
	 * <p>
	 * The highlighter type must be the same as the default highlighter's, if a {@link #highlighter(Function) default highlighter settings was configured} for this query.
	 *
	 * @param highlighterName name of the highlighter. Can be referenced when defining a {@link HighlightProjectionOptionsStep#highlighter(String) highlight projection}.
	 * @param highlighterContributor a function that will use a highlighter factory to configure the named highlighter.
	 * @return {@code this}, for method chaining.
	 *
	 * @see SearchProjectionFactory#highlight(String)
	 * @see HighlightProjectionOptionsStep#highlighter(String)
	 */
	@Incubating
	S highlighter(String highlighterName,
			Function<? super SearchHighlighterFactory, ? extends HighlighterFinalStep> highlighterContributor);

	/**
	 * Configure a named highlighter.
	 * <p>
	 * Named highlighters are to be referenced in {@link HighlightProjectionOptionsStep#highlighter(String) highlight projections}.
	 * <p>
	 * Named highlighters override the settings set by a default highlighter if such was configured for this query.
	 * <p>
	 * The highlighter type must be the same as the default highlighter's, if a {@link #highlighter(Function) default highlighter settings was configured} for this query.
	 *
	 * @param highlighterName name of the highlighter. Can be referenced when defining a {@link HighlightProjectionOptionsStep#highlighter(String) highlight projection}.
	 * @param highlighter a highlighter obtained from the search scope that will serve as the named highlighter.
	 * @return {@code this}, for method chaining.
	 *
	 * @see SearchProjectionFactory#highlight(String)
	 * @see HighlightProjectionOptionsStep#highlighter(String)
	 */
	@Incubating
	S highlighter(String highlighterName, SearchHighlighter highlighter);
}
