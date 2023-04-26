/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.util.common.SearchException;

/**
 * The initial step in a query definition, where the element used to represent query hits,
 * i.e. the "SELECT" clause, can be set.
 * <p>
 * The "SELECT" clause may be omitted by setting the {@link #where(Function) "WHERE" clause} directly,
 * in which case {@link #selectEntity()} will be assumed.
 *
 * @param <N> The next step if no type of hits is explicitly selected,
 * i.e. if {@link #where(SearchPredicate)} or {@link #where(Function)} is called directly
 * without calling {@link #selectEntity()}, or {@link #selectEntityReference()}, {@link #select(SearchProjection)}
 * or a similar method.
 * @param <R> The type of entity references, i.e. the type of hits returned by
 * {@link #selectEntityReference() reference queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entityReference() entity reference projections}.
 * @param <E> The type of entities, i.e. the type of hits returned by
 * {@link #selectEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 * @param <LOS> The type of the initial step of the loading options definition DSL accessible through {@link SearchQueryOptionsStep#loading(Consumer)}.
 * @param <PJF> The type of factory used to create projections in {@link #select(Function)}.
 * @param <PDF> The type of factory used to create predicates in {@link #where(Function)}.
 */
public interface SearchQuerySelectStep<
				N extends SearchQueryOptionsStep<?, E, LOS, ?, ?>,
				R,
				E,
				LOS,
				PJF extends SearchProjectionFactory<R, E>,
				PDF extends SearchPredicateFactory
		>
		extends SearchQueryWhereStep<N, E, LOS, PDF> {

	/**
	 * Select the entity was originally indexed
	 * as a representation of the search hit for each matching document.
	 * <p>
	 * The entity will be loaded directly from its original source (relational database, ...).
	 *
	 * @return The next step.
	 * @see SearchQueryWhereStep
	 */
	SearchQueryWhereStep<?, E, LOS, ?> selectEntity();

	/**
	 * Select a reference to the entity that was originally indexed
	 * as a representation of the search hit for each matching document.
	 * <p>
	 * Entity references are instances of type {@link EntityReference},
	 * but some mappers may expose a different type for backwards compatibility reasons.
	 * {@link EntityReference} should be favored wherever possible
	 * as mapper-specific types will eventually be removed.
	 *
	 * @return The next step.
	 * @see SearchQueryWhereStep
	 */
	SearchQueryWhereStep<?, R, LOS, ?> selectEntityReference();

	/**
	 * Select an object projection
	 * as a representation of the search hit for each matching document.
	 *
	 * @param objectClass The type of objects returned by the projection.
	 * The class is expected to be mapped (generally through annotations)
	 * in such a way that it defines the inner projections.
	 * @param <P> The resulting type of the projection.
	 * @return The next step.
	 * @see SearchQueryWhereStep
	 */
	<P> SearchQueryWhereStep<?, P, LOS, ?> select(Class<P> objectClass);

	/**
	 * Select a given projection as a representation of the search hit for each matching document.
	 *
	 * @param projectionContributor A function that will use the factory passed in parameter to create a projection,
	 * returning the final step in the projection DSL.
	 * Should generally be a lambda expression.
	 * @param <P> The resulting type of the projection.
	 * @return The next step.
	 * @see SearchQueryWhereStep
	 */
	<P> SearchQueryWhereStep<?, P, LOS, ?> select(
			Function<? super PJF, ? extends ProjectionFinalStep<P>> projectionContributor);

	/**
	 * Select a projection as a representation of the search hit for each matching document.
	 *
	 * @param projection A previously-created {@link SearchProjection} object.
	 * @param <P> The resulting type of the projection.
	 * @return The next step.
	 * @see SearchQueryWhereStep
	 */
	<P> SearchQueryWhereStep<?, P, LOS, ?> select(SearchProjection<P> projection);

	/**
	 * Select a list of projections as a representation of the search hit for each matching document.
	 * <p>
	 * Note that using this method will force you to use casts when consuming the results,
	 * since the returned lists are not typed ({@code List<?>} instead of {@code List<T>}).
	 * You can replace calls to this method advantageously with calls to {@link #select(Function)}
	 * defining a {@link SearchProjectionFactory#composite() composite projection}.
	 *
	 * @param projections A list of previously-created {@link SearchProjection} objects.
	 * @return The next step.
	 * @see SearchProjectionFactory#composite(SearchProjection[])
	 * @see SearchQueryWhereStep
	 */
	SearchQueryWhereStep<?, List<?>, LOS, ?> select(SearchProjection<?>... projections);

	/**
	 * Extend the current DSL step with the given extension,
	 * resulting in an extended step offering more query options.
	 *
	 * @param extension The extension to the query DSL.
	 * @param <T> The type of DSL step provided by the extension.
	 * @return The extended DSL step.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchQueryDslExtension<T, R, E, LOS> extension);

}
