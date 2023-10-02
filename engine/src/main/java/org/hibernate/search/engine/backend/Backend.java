/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend;

import java.util.Optional;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A backend as viewed by Hibernate Search users.
 * <p>
 * This interface exposes all operations that Hibernate Search users
 * should be able to execute directly on the backend, without having to go through mapper-specific APIs.
 */
public interface Backend {

	// TODO HSEARCH-3589 add standard APIs related to analysis (which is backend-scoped). To test if an analyzer is defined, for example.
	// TODO HSEARCH-3129 add standard APIs related to statistics?

	/**
	 * Unwrap the backend to some implementation-specific type.
	 *
	 * @param clazz The {@link Class} representing the expected type
	 * @param <T> The expected type
	 * @return The unwrapped backend.
	 * @throws SearchException if the backend implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> clazz);

	/**
	 * @return The name of the backend. In case of a default backend - {@link Optional#empty() an empty optional}.
	 */
	@Incubating
	Optional<String> name();
}
