/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import org.hibernate.search.engine.search.DocumentReference;

/**
 * Contract binding result hits and the mapper.
 */
public interface ProjectionHitMapper<R, O> {

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
	 * Loads the entities planned for loading in one go.
	 *
	 * @return The loaded entities.
	 */
	LoadingResult<O> load();
}
