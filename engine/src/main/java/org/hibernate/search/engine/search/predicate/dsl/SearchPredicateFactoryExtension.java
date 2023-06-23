/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Optional;

/**
 * An extension to the search predicate DSL, allowing the use of non-standard predicates in a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended predicate factories. Should generally extend
 * {@link SearchPredicateFactory}.
 *
 * @see SearchPredicateFactory#extension(SearchPredicateFactoryExtension)
 * @see ExtendedSearchPredicateFactory
 */
public interface SearchPredicateFactoryExtension<T> {

	/**
	 * Attempt to extend a given factory, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchPredicateFactory}.
	 * @return An optional containing the extended search predicate factory ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(SearchPredicateFactory original);

}
