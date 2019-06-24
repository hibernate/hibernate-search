/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection;

import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;

/**
 * The DSL step when attempting to apply multiple extensions
 * to a {@link SearchProjectionFactoryContext}.
 *
 * @param <R> The type of entity references in the parent {@link SearchProjectionFactoryContext}.
 * @param <E> The type of entities in the parent {@link SearchProjectionFactoryContext}.
 * @param <P> The resulting projection type.
 *
 * @see SearchProjectionFactoryContext#extension()
 */
public interface SearchProjectionFactoryContextExtensionStep<P, R, E> {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchProjectionFactoryContextExtension, Function)}
	 * was supported, extend the current context with this extension,
	 * apply the given function to the extended context, and store the resulting projection for later retrieval.
	 * <p>
	 * This method cannot be called after {@link #orElse(Function)} or {@link #orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param projectionContributor A function called if the extension is successfully applied;
	 * it will use the (extended) DSL context passed in parameter to create a projection,
	 * returning the final step in the projection DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended context.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchProjectionFactoryContextExtensionStep<P, R, E> ifSupported(
			SearchProjectionFactoryContextExtension<T, R, E> extension,
			Function<T, ? extends ProjectionFinalStep<P>> projectionContributor
	);

	/**
	 * If no extension passed to {@link #ifSupported(SearchProjectionFactoryContextExtension, Function)}
	 * was supported so far, apply the given function to the current (non-extended) {@link SearchProjectionFactoryContext};
	 * otherwise return the projection created in the first succeeding {@code ifSupported} call.
	 *
	 * @param projectionContributor A function called if no extension was successfully applied;
	 * it will use the (extended) DSL context passed in parameter to create a projection,
	 * returning the final step in the projection DSL.
	 * Should generally be a lambda expression.
	 * @return The created projection.
	 */
	ProjectionFinalStep<P> orElse(Function<SearchProjectionFactoryContext<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchProjectionFactoryContextExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the projection created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The created projection.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	ProjectionFinalStep<P> orElseFail();

}
