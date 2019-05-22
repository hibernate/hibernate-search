/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.util.common.SearchException;

/**
 * The context used when building a query, before the search result type has been defined.
 *
 * @param <N> The next context if no type of hits is explicitly selected,
 * i.e. if {@link #predicate(SearchPredicate)} or {@link #predicate(Function)} is called directly
 * without calling {@link #asEntity()}, or {@link #asReference()}, {@link #asProjection(SearchProjection)}
 * or a similar method.
 * @param <R> The type of references, i.e. the type of hits returned by
 * {@link #asReference() reference queries},
 * or the type of objects returned for {@link SearchProjectionFactoryContext#reference() reference projections}.
 * @param <E> The type of entities, i.e. the type of hits returned by
 * {@link #asEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactoryContext#entity() entity projections}.
 * @param <PJC> The type of contexts used to create projections in {@link #asProjection(Function)}.
 * @param <PDC> The type of contexts used to create predicates in {@link #predicate(Function)}.
 */
public interface SearchQueryResultDefinitionContext<
				N extends SearchQueryContext<?, E, ?>,
				R,
				E,
				PJC extends SearchProjectionFactoryContext<R, E>,
				PDC extends SearchPredicateFactoryContext
		>
		extends SearchQueryResultContext<N, E, PDC> {

	/**
	 * Define the query results as the entity was originally indexed, loaded from an external source (database, ...).
	 *
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	SearchQueryResultContext<?, E, ?> asEntity();

	/**
	 * Define the query results as a reference to entity that was originally indexed.
	 *
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	SearchQueryResultContext<?, R, ?> asReference();

	/**
	 * Define the query results as one projection for each matching document.
	 *
	 * @param projectionContributor A function that will use the DSL context passed in parameter to create a projection,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @param <P> The resulting type of the projection.
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	<P> SearchQueryResultContext<?, P, ?> asProjection(
			Function<? super PJC, ? extends SearchProjectionTerminalContext<P>> projectionContributor);

	/**
	 * Define the query results as one projection for each matching document.
	 *
	 * @param projection A previously-created {@link SearchProjection} object.
	 * @param <P> The resulting type of the projection.
	 * @return A context allowing to define the query further.
	 * @see SearchQueryResultContext
	 */
	<P> SearchQueryResultContext<?, P, ?> asProjection(SearchProjection<P> projection);

	/**
	 * Define the query results as a list of projections for each matching document.
	 * <p>
	 * Note that using this method will force you to use casts when consuming the results,
	 * since the returned lists are not typed ({@code List<?>} instead of {@code List<T>}).
	 * You can replace calls to this method advantageously with calls to {@link #asProjection(Function)}
	 * defining a {@link SearchProjectionFactoryContext#composite(BiFunction, SearchProjection, SearchProjection) composite projection}.
	 *
	 * @param projections A list of previously-created {@link SearchProjection} objects.
	 * @return A context allowing to define the query further.
	 * @see SearchProjectionFactoryContext#composite(BiFunction, SearchProjection, SearchProjection)
	 * @see SearchQueryResultContext
	 */
	SearchQueryResultContext<?, List<?>, ?> asProjections(SearchProjection<?>... projections);

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more query options.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchQueryContextExtension<T, R, E> extension);

}
