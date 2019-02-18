/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import java.util.function.Consumer;

import org.hibernate.search.util.common.SearchException;

/**
 * The context used when attempting to apply multiple extensions
 * to a {@link SearchSortContainerContext}.
 *
 * @see SearchSortContainerContext#extension()
 */
public interface SearchSortContainerExtensionContext {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchSortContainerContextExtension, Consumer)}
	 * was supported, extend the current context with this extension,
	 * and apply the given consumer to the extended context.
	 * <p>
	 * This method cannot be called after {@link #orElse(Consumer)} or {@link #orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param sortContributor A consumer that will add a sort to the (extended) context passed in parameter,
	 * if the extension is successfully applied.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended context.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchSortContainerExtensionContext ifSupported(
			SearchSortContainerContextExtension<T> extension, Consumer<T> sortContributor
	);

	/**
	 * If no extension passed to {@link #ifSupported(SearchSortContainerContextExtension, Consumer)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchSortContainerContext};
	 * otherwise do nothing.
	 *
	 * @param sortContributor A consumer that will add a sort to the (non-extended) {@link SearchSortContainerContext}.
	 * Should generally be a lambda expression.
	 * @return The next context.
	 */
	NonEmptySortContext orElse(Consumer<SearchSortContainerContext> sortContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchSortContainerContextExtension, Consumer)}
	 * was supported so far, throw an exception; otherwise do nothing.
	 *
	 * @return The next context.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	NonEmptySortContext orElseFail();

}
