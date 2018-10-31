/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;

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
	 * and apply the given consumer to the extended context.
	 * <p>
	 * This method cannot be called after {@link #orElse(Function)} or {@link #orElseFail()}.
	 *
	 * @param extension The extension to apply.
	 * @param predicateContributor A function that will use the (extended) context passed in parameter to create a {@link SearchPredicate},
	 * if the extension is successfully applied.
	 * Should generally be a lambda expression.
	 * @param <T> The type of the extended context.
	 * @return {@code this}, for method chaining.
	 */
	<T> SearchPredicateFactoryExtensionContext ifSupported(
			SearchPredicateFactoryContextExtension<T> extension, Function<T, SearchPredicate> predicateContributor
	);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported so far, apply the given consumer to the current (non-extended) {@link SearchPredicateFactoryContext};
	 * otherwise do nothing.
	 *
	 * @param predicateContributor A function that will use the (non-extended) context passed in parameter to create a {@link SearchPredicate},
	 * if the extension is successfully applied.
	 * Should generally be a lambda expression.
	 */
	SearchPredicate orElse(Function<SearchPredicateFactoryContext, SearchPredicate> predicateContributor);

	/**
	 * If no extension passed to {@link #ifSupported(SearchPredicateFactoryContextExtension, Function)}
	 * was supported so far, throw an exception; otherwise do nothing.
	 *
	 * @throws org.hibernate.search.util.SearchException If none of the previously passed extensions was supported.
	 */
	SearchPredicate orElseFail();

}
