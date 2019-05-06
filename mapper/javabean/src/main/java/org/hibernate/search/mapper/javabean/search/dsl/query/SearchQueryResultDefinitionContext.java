/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.dsl.query;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.mapper.javabean.search.SearchScope;
import org.hibernate.search.mapper.javabean.search.query.SearchQuery;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface SearchQueryResultDefinitionContext {

	/**
	 * Define the query results as one {@link PojoReference} for each matching document.
	 *
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	SearchQueryResultContext<?, SearchQuery<PojoReference>, ?> asReference();

	/**
	 * Define the query results as one projection for each matching document.
	 *
	 * @param projectionContributor A function that will use the DSL context passed in parameter to create a projection,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @param <T> The resulting type of the projection.
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	<T> SearchQueryResultContext<?, SearchQuery<T>, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, ?>, ? extends SearchProjectionTerminalContext<T>> projectionContributor);

	/**
	 * Define the query results as one projection for each matching document.
	 *
	 * @param projection A {@link SearchProjection} object obtained from the {@link SearchScope}.
	 * @param <T> The resulting type of the projection.
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	<T> SearchQueryResultContext<?, SearchQuery<T>, ?> asProjection(SearchProjection<T> projection);

	/**
	 * Define the query results as a list of projections for each matching document.
	 * <p>
	 * Note that using this method will force you to use casts when consuming the results,
	 * since the returned lists are not typed ({@code List<?>} instead of {@code List<T>}).
	 * You can replace calls to this method advantageously with calls to {@link #asProjection(Function)}
	 * defining a {@link SearchProjectionFactoryContext#composite(BiFunction, SearchProjection, SearchProjection) composite projection}.
	 *
	 * @param projections A list of {@link SearchProjection} object obtained from the {@link SearchScope}.
	 * @return A context allowing to define the query further.
	 * @see SearchProjectionFactoryContext#composite(BiFunction, SearchProjection, SearchProjection)
	 * @see SearchQueryResultContext
	 */
	SearchQueryResultContext<?, SearchQuery<List<?>>, ?> asProjections(SearchProjection<?>... projections);
}
