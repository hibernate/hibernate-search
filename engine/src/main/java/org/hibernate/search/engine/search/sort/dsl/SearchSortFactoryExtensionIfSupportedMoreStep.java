/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;

/**
 * The second and later step when attempting to apply multiple extensions
 * to a {@link SearchSortFactory}.
 *
 * @see SearchSortFactory#extension()
 */
public interface SearchSortFactoryExtensionIfSupportedMoreStep<E> extends SearchSortFactoryExtensionIfSupportedStep<E> {

	/**
	 * If no extension passed to {@link #ifSupported(SearchSortFactoryExtension, Function)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchSortFactory};
	 * otherwise return the sort created in the first succeeding {@code ifSupported} call.
	 *
	 * @param sortContributor A function called if no extension was successfully applied;
	 * it will use the (non-extended) sort factory passed in parameter to create a sort,
	 * returning the final step in the sort DSL.
	 * Should generally be a lambda expression.
	 * @return The final step in the DSL of the resulting sort.
	 */
	SortThenStep<E> orElse(Function<SearchSortFactory<E>, ? extends SortFinalStep> sortContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchSortFactoryExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the sort created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The final step in the DSL of the resulting sort.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	SortThenStep<E> orElseFail();

}
