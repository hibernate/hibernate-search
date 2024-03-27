/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.Optional;

/**
 * An extension to the search projection DSL, allowing the use of non-standard projections in a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended projection factories. Should generally extend
 * {@link SearchProjectionFactory}.
 * @param <R> The type of entity references in the original {@link SearchProjectionFactory}.
 * @param <E> The type of entities in the original {@link SearchProjectionFactory}.
 *
 * @see SearchProjectionFactory#extension(SearchProjectionFactoryExtension)
 * @see ExtendedSearchProjectionFactory
 */
public interface SearchProjectionFactoryExtension<T, R, E> {

	/**
	 * Attempt to extend a given factory, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchProjectionFactory}.
	 * @return An optional containing the extended projection factory ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(SearchProjectionFactory<R, E> original);

}
