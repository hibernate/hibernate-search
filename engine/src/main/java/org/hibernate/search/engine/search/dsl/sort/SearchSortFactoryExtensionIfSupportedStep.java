/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import java.util.function.Function;

/**
 * The initial step when attempting to apply multiple extensions
 * to a {@link SearchSortFactory}.
 *
 * @see SearchSortFactory#extension()
 */
public interface SearchSortFactoryExtensionIfSupportedStep {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchSortFactoryExtension, Function)}
	 * was supported, extend the current factory with this extension,
	 * apply the given function to the extended factory, and store the resulting sort for later retrieval.
	 * <p>
	 * This method cannot be called after {@link SearchSortFactoryExtensionIfSupportedMoreStep#orElse(Function)}
	 * or {@link SearchSortFactoryExtensionIfSupportedMoreStep#orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param sortContributor A function called if the extension is successfully applied;
	 * it will use the (extended) sort factory passed in parameter to create a sort,
	 * returning the final step in the sort DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended factory.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchSortFactoryExtensionIfSupportedMoreStep ifSupported(
			SearchSortFactoryExtension<T> extension,
			Function<T, ? extends SortFinalStep> sortContributor
	);


}
