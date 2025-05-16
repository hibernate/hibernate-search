/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;

/**
 * The second and later step when attempting to apply multiple extensions
 * to a {@link TypedSearchProjectionFactory}.
 *
 * @param <SR> Scope root type.
 * @param <R> The type of entity references in the parent {@link TypedSearchProjectionFactory}.
 * @param <E> The type of entities in the parent {@link TypedSearchProjectionFactory}.
 * @param <P> The resulting projection type.
 *
 * @see TypedSearchProjectionFactory#extension()
 */
public interface SearchProjectionFactoryExtensionIfSupportedMoreStep<SR, P, R, E>
		extends SearchProjectionFactoryExtensionIfSupportedStep<SR, P, R, E> {

	/**
	 * If no extension passed to {@link #ifSupported(SearchProjectionFactoryExtension, Function)}
	 * was supported so far, apply the given function to the current (non-extended) {@link TypedSearchProjectionFactory};
	 * otherwise return the projection created in the first succeeding {@code ifSupported} call.
	 *
	 * @param projectionContributor A function called if no extension was successfully applied;
	 * it will use the (non-extended) projection factory passed in parameter to create a projection,
	 * returning the final step in the projection DSL.
	 * Should generally be a lambda expression.
	 * @return The created projection.
	 */
	ProjectionFinalStep<P> orElse(
			Function<TypedSearchProjectionFactory<SR, R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchProjectionFactoryExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the projection created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The created projection.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	ProjectionFinalStep<P> orElseFail();

}
