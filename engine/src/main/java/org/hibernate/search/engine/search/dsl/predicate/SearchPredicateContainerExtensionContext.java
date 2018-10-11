/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Consumer;

/**
 * The context used when attempting to apply multiple extensions
 * to a {@link SearchPredicateContainerContext}.
 *
 * @param <N> The type of the next context (returned by {@link #orElse(Consumer)} for example).
 * @see SearchPredicateContainerContext#extension()
 */
public interface SearchPredicateContainerExtensionContext<N> {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchPredicateContainerContextExtension, Consumer)}
	 * was supported, extend the current context with this extension,
	 * and apply the given consumer to the extended context.
	 * <p>
	 * This method cannot be called after {@link #orElse(Consumer)} or {@link #orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param predicateContributor A consumer that will add a predicate to the (extended) context passed in parameter,
	 * if the extension is successfully applied.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended context.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchPredicateContainerExtensionContext<N> ifSupported(
			SearchPredicateContainerContextExtension<N, T> extension, Consumer<T> predicateContributor
	);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateContainerContextExtension, Consumer)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchPredicateContainerContext};
	 * otherwise do nothing.
	 *
	 * @param predicateContributor A consumer that will add a predicate to the (non-extended) {@link SearchPredicateContainerContext}.
	 * Should generally be a lambda expression.
	 * @return The next context.
	 */
	N orElse(Consumer<SearchPredicateContainerContext<?>> predicateContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateContainerContextExtension, Consumer)}
	 * was supported so far, throw an exception; otherwise do nothing.
	 *
	 * @return The next context.
	 * @throws org.hibernate.search.util.SearchException If none of the previously passed extensions was supported.
	 */
	N orElseFail();

}
