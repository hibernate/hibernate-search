/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping;

/**
 * An extension to the search mapping, providing an extended search mapping offering mapper-specific utilities.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended search mapping.
 * Should generally extend {@link SearchMapping}.
 *
 * @see SearchMapping#extension(SearchMappingExtension)
 */
public interface SearchMappingExtension<T> {

	/**
	 * Attempt to extend a search mapping, throwing an exception in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link SearchMapping}.
	 * @return An extended search mapping ({@link T})
	 */
	T extendOrFail(SearchMapping original);

}
