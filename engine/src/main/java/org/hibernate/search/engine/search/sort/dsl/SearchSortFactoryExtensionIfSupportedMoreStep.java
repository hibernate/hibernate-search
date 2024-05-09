/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;

/**
 * The second and later step when attempting to apply multiple extensions
 * to a {@link SearchSortFactory}.
 *
 * @param <SR> Scope root type.
 * @see SearchSortFactory#extension()
 */
public interface SearchSortFactoryExtensionIfSupportedMoreStep<SR> extends SearchSortFactoryExtensionIfSupportedStep<SR> {

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
	SortThenStep<SR> orElse(Function<SearchSortFactory<SR>, ? extends SortFinalStep> sortContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchSortFactoryExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the sort created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The final step in the DSL of the resulting sort.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	SortThenStep<SR> orElseFail();

}
