/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.Optional;

public interface ProjectionRegistry {

	/**
	 * @param objectClass The type of objects returned by the projection.
	 * The class is expected to be mapped (generally through annotations)
	 * in such a way that it defines the inner projections.
	 * @param <T> The type of objects returned by the projection.
	 * @return A definition of the projection.
	 * @throws org.hibernate.search.util.common.SearchException If no projection definition exists for this object class.
	 */
	<T> CompositeProjectionDefinition<T> composite(Class<T> objectClass);

	/**
	 * @param objectClass The type of objects returned by the projection.
	 * The class is expected to be mapped (generally through annotations)
	 * in such a way that it defines the inner projections.
	 * @param <T> The type of objects returned by the projection.
	 * @return A definition of the projection,
	 * or {@link Optional#empty()} if no projection definition is available for the given object class.
	 */
	<T> Optional<CompositeProjectionDefinition<T>> compositeOptional(Class<T> objectClass);

}
