/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;


/**
 * The result of the loading of the entities by the {@link ProjectionHitMapper}.
 */
public interface LoadingResult<O> {

	/**
	 * @return The loaded entity corresponding to the key.
	 */
	O getLoaded(Object key);
}
