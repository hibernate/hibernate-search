/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

import java.util.Optional;

import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;

/**
 * An extension to the search query, allowing to wrap a query.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <Q> The type of search queries wrappers created by this extension.
 * @param <H> The type of hits in the query to be wrapped.
 *
 * @see org.hibernate.search.engine.search.query.SearchQuery#extension(SearchQueryExtension)
 */
public interface SearchQueryExtension<Q, H> {

	/**
	 * Attempt to extend a given query, returning an empty {@link Optional} in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link org.hibernate.search.engine.search.query.SearchQuery}.
	 * @param loadingContext The {@link SearchLoadingContext} used by the original query.
	 * @return An optional containing the extended search query ({@link Q}) in case
	 * of success, or an empty optional otherwise.
	 */
	Optional<Q> extendOptional(SearchQuery<H> original, SearchLoadingContext<?> loadingContext);

}
