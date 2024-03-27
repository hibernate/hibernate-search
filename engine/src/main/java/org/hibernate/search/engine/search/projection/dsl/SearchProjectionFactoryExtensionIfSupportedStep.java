/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.function.Function;

/**
 * The initial step when attempting to apply multiple extensions
 * to a {@link SearchProjectionFactory}.
 *
 * @param <R> The type of entity references in the parent {@link SearchProjectionFactory}.
 * @param <E> The type of entities in the parent {@link SearchProjectionFactory}.
 * @param <P> The resulting projection type.
 *
 * @see SearchProjectionFactory#extension()
 */
public interface SearchProjectionFactoryExtensionIfSupportedStep<P, R, E> {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchProjectionFactoryExtension, Function)}
	 * was supported, extend the current factory with this extension,
	 * apply the given function to the extended factory, and store the resulting projection for later retrieval.
	 * <p>
	 * This method cannot be called after {@link SearchProjectionFactoryExtensionIfSupportedMoreStep#orElse(Function)}
	 * or {@link SearchProjectionFactoryExtensionIfSupportedMoreStep#orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param projectionContributor A function called if the extension is successfully applied;
	 * it will use the (extended) projection factory passed in parameter to create a projection,
	 * returning the final step in the projection DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended factory.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchProjectionFactoryExtensionIfSupportedMoreStep<P, R, E> ifSupported(
			SearchProjectionFactoryExtension<T, R, E> extension,
			Function<T, ? extends ProjectionFinalStep<P>> projectionContributor
	);

}
