/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;

/**
 * The second and later step when attempting to apply multiple extensions
 * to a {@link SearchPredicateFactory}.
 *
 * @see SearchPredicateFactory#extension()
 */
public interface SearchPredicateFactoryExtensionIfSupportedMoreStep
		extends SearchPredicateFactoryExtensionIfSupportedStep {

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryExtension, Function)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchPredicateFactory};
	 * otherwise return the predicate created in the first succeeding {@code ifSupported} call.
	 *
	 * @param predicateContributor A function called if no extension was successfully applied;
	 * it will use the (non-extended) predicate factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return The final step in the DSL of the resulting predicate.
	 */
	PredicateFinalStep orElse(
			Function<SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the predicate created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The final step in the DSL of the resulting predicate.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	PredicateFinalStep orElseFail();

}
