/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Optional;

/**
 * An extension to the search aggregation DSL, allowing the use of non-standard aggregations in a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended aggregation factories. Should generally extend
 * {@link TypedSearchAggregationFactory}.
 *
 * @see TypedSearchAggregationFactory#extension(SearchAggregationFactoryExtension)
 * @see ExtendedSearchAggregationFactory
 */
public interface SearchAggregationFactoryExtension<T> {

	/**
	 * Attempt to extend a given factory, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchAggregationFactory}.
	 * @return An optional containing the extended aggregation factory ({@link T}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<T> extendOptional(SearchAggregationFactory original);

}
