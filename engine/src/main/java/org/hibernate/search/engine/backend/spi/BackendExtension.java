/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.spi;


import org.hibernate.search.engine.backend.Backend;

/**
 * An extension to the {@link org.hibernate.search.engine.backend.Backend} API,
 * allowing to execute non-standard operations or retrieve non-standard information from a Backend.
 *
 * @param <T> The type of extended {@link org.hibernate.search.engine.backend.Backend}.
 *
 * @see Backend#withExtension(BackendExtension)
 */
public interface BackendExtension<T extends Backend> {

	/**
	 * Attempt to extend a given backend, throwing an exception in case of failure.
	 *
	 * @param original The original, non-extended {@link Backend}.
	 * @return An extended backend ({@link T})
	 * @throws org.hibernate.search.util.SearchException If the current extension does not support the given
	 * backend (incompatible technology).
	 */
	T extendOrFail(Backend original);

}
