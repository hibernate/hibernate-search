/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

import org.hibernate.search.engine.backend.common.DocumentReference;

/**
 * Contract binding result hits and the mapper.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of entities.
 */
public interface ProjectionHitMapper<R, E> {

	/**
	 * Convert a document reference to the reference specific to the mapper.
	 *
	 * @param reference The document reference.
	 * @return The reference specific to the mapper.
	 */
	R convertReference(DocumentReference reference);

	/**
	 * Plan the loading of an entity.
	 *
	 * @param reference The document reference.
	 * @return The key to use to retrieve the loaded entity from {@link LoadingResult} after load.
	 */
	Object planLoading(DocumentReference reference);

	/**
	 * Loads the entities planned for loading in one go, blocking the current thread while doing so.
	 *
	 * @return The loaded entities.
	 */
	LoadingResult<E> loadBlocking();

}
