/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.util.common.SearchException;

/**
 * The initial step in a query definition, where the type of query hits can be set,
 * or where the predicate can be set directly, assuming that query hits are returned as entities.
 *
 * @param <N> The next step if no type of hits is explicitly selected,
 * i.e. if {@link #predicate(SearchPredicate)} or {@link #predicate(Function)} is called directly
 * without calling {@link #asEntity()}, or {@link #asEntityReference()}, {@link #asProjection(SearchProjection)}
 * or a similar method.
 * @param <R> The type of entity references, i.e. the type of hits returned by
 * {@link #asEntityReference() reference queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entityReference() entity reference projections}.
 * @param <E> The type of entities, i.e. the type of hits returned by
 * {@link #asEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 * @param <PJF> The type of factory used to create projections in {@link #asProjection(Function)}.
 * @param <PDF> The type of factory used to create predicates in {@link #predicate(Function)}.
 */
public interface SearchQueryHitTypeStep<
				N extends SearchQueryOptionsStep<?, E, ?, ?>,
				R,
				E,
				PJF extends SearchProjectionFactory<R, E>,
				PDF extends SearchPredicateFactory
		>
		extends SearchQueryPredicateStep<N, E, PDF> {

	/**
	 * Define the query results as the entity was originally indexed, loaded from an external source (database, ...).
	 *
	 * @return The next step.
	 * @see SearchQueryPredicateStep
	 */
	SearchQueryPredicateStep<?, E, ?> asEntity();

	/**
	 * Define the query results as a reference to the entity that was originally indexed.
	 *
	 * @return The next step.
	 * @see SearchQueryPredicateStep
	 */
	SearchQueryPredicateStep<?, R, ?> asEntityReference();

	/**
	 * Define the query results as one projection for each matching document.
	 *
	 * @param projectionContributor A function that will use the factory passed in parameter to create a projection,
	 * returning the final step in the projection DSL.
	 * Should generally be a lambda expression.
	 * @param <P> The resulting type of the projection.
	 * @return The next step.
	 * @see SearchQueryPredicateStep
	 */
	<P> SearchQueryPredicateStep<?, P, ?> asProjection(
			Function<? super PJF, ? extends ProjectionFinalStep<P>> projectionContributor);

	/**
	 * Define the query results as one projection for each matching document.
	 *
	 * @param projection A previously-created {@link SearchProjection} object.
	 * @param <P> The resulting type of the projection.
	 * @return The next step.
	 * @see SearchQueryPredicateStep
	 */
	<P> SearchQueryPredicateStep<?, P, ?> asProjection(SearchProjection<P> projection);

	/**
	 * Define the query results as a list of projections for each matching document.
	 * <p>
	 * Note that using this method will force you to use casts when consuming the results,
	 * since the returned lists are not typed ({@code List<?>} instead of {@code List<T>}).
	 * You can replace calls to this method advantageously with calls to {@link #asProjection(Function)}
	 * defining a {@link SearchProjectionFactory#composite(BiFunction, SearchProjection, SearchProjection) composite projection}.
	 *
	 * @param projections A list of previously-created {@link SearchProjection} objects.
	 * @return The next step.
	 * @see SearchProjectionFactory#composite(BiFunction, SearchProjection, SearchProjection)
	 * @see SearchQueryPredicateStep
	 */
	SearchQueryPredicateStep<?, List<?>, ?> asProjections(SearchProjection<?>... projections);

	/**
	 * Extend the current DSL step with the given extension,
	 * resulting in an extended step offering more query options.
	 *
	 * @param extension The extension to the query DSL.
	 * @param <T> The type of DSL step provided by the extension.
	 * @return The extended DSL step.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchQueryDslExtension<T, R, E> extension);

}
