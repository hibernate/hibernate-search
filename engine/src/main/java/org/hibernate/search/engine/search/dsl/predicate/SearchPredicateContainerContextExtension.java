/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import java.util.Optional;

import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

/**
 * An extension to the search query DSL, allowing to add non-standard predicates to a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended search container contexts. Should generally extend
 * {@link SearchPredicateContainerContext}.
 *
 * @see SearchPredicateContainerContext#extension(SearchPredicateContainerContextExtension)
 * @see DelegatingSearchPredicateContainerContextImpl
 */
public interface SearchPredicateContainerContextExtension<T> {

	/**
	 * Attempt to extend a given context, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param <C> The type of query element collector for the given DSL context.
	 * @param <B> The implementation type of builders for the given DSL context.
	 * @param original The original, non-extended {@link SearchPredicateContainerContext}.
	 * @param factory A {@link SearchPredicateFactory}.
	 * @return An optional containing the extended search predicate container context ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	<C, B> Optional<T> extendOptional(SearchPredicateContainerContext original,
			SearchPredicateFactory<C, B> factory);

}
