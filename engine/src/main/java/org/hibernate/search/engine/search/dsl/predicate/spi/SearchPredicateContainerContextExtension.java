/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;


import java.util.Optional;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

/**
 * An extension to the search query DSL, allowing to add non-standard predicates to a query.
 *
 * @param <N> The next context type
 * @param <T> The type of extended search container contexts. Should generally extend
 * {@link SearchPredicateContainerContext}.
 *
 * @see SearchPredicateContainerContext#withExtension(SearchPredicateContainerContextExtension)
 * @see DelegatingSearchPredicateContainerContextImpl
 */
public interface SearchPredicateContainerContextExtension<N, T> {

	/**
	 * Attempt to extend a given context, throwing an exception in case of failure.
	 *
	 * @param original The original, non-extended {@link SearchPredicateContainerContext}.
	 * @param factory A {@link SearchPredicateFactory}.
	 * @param dslContext A {@link SearchPredicateDslContext}.
	 * @param <CTX> The type of the context passed to the contribution method of the contributors.
	 * @param <C> The type of collector expected by search predicate contributors for the given search target.
	 * @return An extended search predicate container context ({@link T})
	 * @throws org.hibernate.search.util.SearchException If the current extension does not support the given
	 * search target (incompatible technology).
	 */
	<CTX, C> T extendOrFail(SearchPredicateContainerContext<N> original,
			SearchPredicateFactory<CTX, C> factory, SearchPredicateDslContext<N, CTX, ? extends C> dslContext);

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 *
	 * @param original The original, non-extended {@link SearchPredicateContainerContext}.
	 * @param factory A {@link SearchPredicateFactory}.
	 * @param dslContext A {@link SearchPredicateDslContext}.
	 * @param <CTX> The type of the context passed to the contribution method of the contributors.
	 * @param <C> The type of collector expected by search predicate contributors for the given search target.
	 * @return An optional containing the extended search predicate container context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	<CTX, C> Optional<T> extendOptional(SearchPredicateContainerContext<N> original,
			SearchPredicateFactory<CTX, C> factory, SearchPredicateDslContext<N, CTX, ? extends C> dslContext);

}
