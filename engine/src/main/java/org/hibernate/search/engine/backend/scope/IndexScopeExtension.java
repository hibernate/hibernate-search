/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.scope;

import org.hibernate.search.engine.backend.scope.spi.IndexScope;

/**
 * An extension to the index scope, providing an extended index scope offering backend-specific utilities.
 * <p>
 * <strong>WARNING:</strong> while this type is API, because instances should be manipulated by users,
 * all of its methods are considered SPIs and therefore should never be called or implemented directly by users.
 * In short, users are only expected to get instances of this type from an API ({@code SomeExtension.get()})
 * and pass it to another API.
 *
 * @param <T> The type of extended index scope.
 * Should generally extend {@link IndexScope}.
 *
 * @see IndexScope#extension(IndexScopeExtension)
 */
public interface IndexScopeExtension<T> {

	/**
	 * Attempt to extend an index scope, throwing an exception in case of failure.
	 * <p>
	 * <strong>WARNING:</strong> this method is not API, see comments at the type level.
	 *
	 * @param original The original, non-extended {@link IndexScope}.
	 * @return An extended index scope ({@link T})
	 */
	T extendOrFail(IndexScope original);

}
