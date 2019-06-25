/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;


import org.hibernate.search.engine.search.DocumentReference;

/**
 * The result of the loading of the entities by the {@link ProjectionHitMapper}.
 *
 * @param <E> The type of entities.
 */
public interface LoadingResult<E> {

	/**
	 * @param key The key that was previously returned by {@link ProjectionHitMapper#planLoading(DocumentReference)}.
	 * @return The loaded entity corresponding to the key, or {@code null} if the entity could not be loaded.
	 */
	E getLoaded(Object key);
}
