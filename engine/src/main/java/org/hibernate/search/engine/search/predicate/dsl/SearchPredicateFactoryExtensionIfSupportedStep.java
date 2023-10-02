/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

/**
 * The initial step when attempting to apply multiple extensions
 * to a {@link SearchPredicateFactory}.
 *
 * @see SearchPredicateFactory#extension()
 */
public interface SearchPredicateFactoryExtensionIfSupportedStep {

	/**
	 * If the given extension is supported, and none of the previous extensions passed to
	 * {@link #ifSupported(SearchPredicateFactoryExtension, Function)}
	 * was supported, extend the current factory with this extension,
	 * apply the given function to the extended factory, and store the resulting predicate for later retrieval.
	 * <p>
	 * This method cannot be called after {@link SearchPredicateFactoryExtensionIfSupportedMoreStep#orElse(Function)}
	 * or {@link SearchPredicateFactoryExtensionIfSupportedMoreStep#orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param predicateContributor A function called if the extension is successfully applied;
	 * it will use the (extended) predicate factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended factory.
	 * @return The next step.
	 */
	<T> SearchPredicateFactoryExtensionIfSupportedMoreStep ifSupported(
			SearchPredicateFactoryExtension<T> extension,
			Function<T, ? extends PredicateFinalStep> predicateContributor
	);

}
