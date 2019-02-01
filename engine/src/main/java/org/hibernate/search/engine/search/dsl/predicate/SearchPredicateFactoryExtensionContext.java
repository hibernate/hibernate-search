/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Function;

/**
 * The context used when attempting to apply multiple extensions
 * to a {@link SearchPredicateFactoryContext}.
 *
 * @see SearchPredicateFactoryContext#extension()
 */
public interface SearchPredicateFactoryExtensionContext {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported, extend the current context with this extension,
	 * apply the given function to the extended context, and store the resulting predicate for later retrieval.
	 * <p>
	 * This method cannot be called after {@link #orElse(Function)} or {@link #orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param predicateContributor A function called if the extension is successfully applied;
	 * it will use the (extended) DSL context passed in parameter to create a predicate,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended context.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchPredicateFactoryExtensionContext ifSupported(
			SearchPredicateFactoryContextExtension<T> extension,
			Function<T, ? extends SearchPredicateTerminalContext> predicateContributor
	);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchPredicateFactoryContext};
	 * otherwise return the predicate created in the first succeeding {@code ifSupported} call.
	 *
	 * @param predicateContributor A function called if no extension was successfully applied;
	 * it will use the (extended) DSL context passed in parameter to create a predicate,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @return The created predicate.
	 */
	SearchPredicateTerminalContext orElse(
			Function<SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> predicateContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the predicate created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The created predicate.
	 * @throws org.hibernate.search.util.SearchException If none of the previously passed extensions was supported.
	 */
	SearchPredicateTerminalContext orElseFail();

}
