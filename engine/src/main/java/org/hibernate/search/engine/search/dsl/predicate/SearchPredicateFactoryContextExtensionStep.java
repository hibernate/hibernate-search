/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Function;

import org.hibernate.search.util.common.SearchException;

/**
 * The DSL step when attempting to apply multiple extensions
 * to a {@link SearchPredicateFactoryContext}.
 *
 * @see SearchPredicateFactoryContext#extension()
 */
public interface SearchPredicateFactoryContextExtensionStep {

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
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended context.
	 * @return The next step.
	 */
	<T> SearchPredicateFactoryContextExtensionStep ifSupported(
			SearchPredicateFactoryContextExtension<T> extension,
			Function<T, ? extends PredicateFinalStep> predicateContributor
	);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchPredicateFactoryContext};
	 * otherwise return the predicate created in the first succeeding {@code ifSupported} call.
	 *
	 * @param predicateContributor A function called if no extension was successfully applied;
	 * it will use the (extended) DSL context passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return The final step in the DSL of the resulting predicate.
	 */
	PredicateFinalStep orElse(
			Function<SearchPredicateFactoryContext, ? extends PredicateFinalStep> predicateContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported so far, throw an exception;
	 * otherwise return the predicate created in the first succeeding {@code ifSupported} call.
	 *
	 * @return The final step in the DSL of the resulting predicate.
	 * @throws SearchException If none of the previously passed extensions was supported.
	 */
	PredicateFinalStep orElseFail();

}
