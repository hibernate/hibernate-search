/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;

/**
 * A factory for search projections.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search projections.
 */
public interface SearchProjectionFactory<P extends SearchProjection<?>> {

	DocumentReferenceSearchProjectionBuilder documentReference();

	<T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> clazz);

	ObjectSearchProjectionBuilder object();

	ReferenceSearchProjectionBuilder reference();

	ScoreSearchProjectionBuilder score();

	DistanceFieldSearchProjectionBuilder distance(String absoluteFieldPath, GeoPoint center);

	/**
	 * Convert a {@link SearchProjection} object to the backend-specific implementation.
	 *
	 * @param projection The {@link SearchProjection} object to convert.
	 * @return The corresponding projection implementation.
	 * @throws SearchException If the {@link SearchProjection} object was created
	 * by a different, incompatible factory.
	 */
	P toImplementation(SearchProjection<?> projection);
}
