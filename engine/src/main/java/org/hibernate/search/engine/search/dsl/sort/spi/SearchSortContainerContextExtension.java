/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;


import java.util.Optional;

import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

/**
 * An extension to the search query DSL, allowing to add non-standard predicates to a query.
 *
 * @param <N> The next context type
 * @param <T> The type of extended search container contexts. Should generally extend
 * {@link SearchSortContainerContext}.
 *
 * @see SearchSortContainerContext#withExtension(SearchSortContainerContextExtension)
 * @see DelegatingSearchSortContainerContextImpl
 */
public interface SearchSortContainerContextExtension<N, T> {

	/**
	 * Attempt to extend a given context, throwing an exception in case of failure.
	 *
	 * @param original The original, non-extended {@link SearchSortContainerContext}.
	 * @param factory A {@link SearchSortFactory}.
	 * @param dslContext A {@link SearchSortDslContext}.
	 * @param <C> The type of query element collector for the given DSL context.
	 * @param <B> The implementation type of builders for the given DSL context.
	 * @return An extended search predicate container context ({@link T})
	 * @throws org.hibernate.search.util.SearchException If the current extension does not support the given
	 * search target (incompatible technology).
	 */
	<C, B> T extendOrFail(
			SearchSortContainerContext<N> original,
			SearchSortFactory<C, B> factory, SearchSortDslContext<N, ? super B> dslContext);

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 *
	 * @param original The original, non-extended {@link SearchSortContainerContext}.
	 * @param factory A {@link SearchSortFactory}.
	 * @param dslContext A {@link SearchSortDslContext}.
	 * @param <C> The type of query element collector for the given DSL context.
	 * @param <B> The implementation type of builders for the given DSL context.
	 * @return An optional containing the extended search predicate container context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	<C, B> Optional<T> extendOptional(
			SearchSortContainerContext<N> original,
			SearchSortFactory<C, B> factory, SearchSortDslContext<N, ? super B> dslContext);

}
