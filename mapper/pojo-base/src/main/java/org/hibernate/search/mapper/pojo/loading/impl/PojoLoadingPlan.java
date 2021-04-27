/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.impl;

import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;

/**
 * A mutable plan to load POJO entities from an external source (database, ...).
 * @param <T> The exposed type of loaded entities.
 */
public interface PojoLoadingPlan<T> {

	/**
	 * Plans the loading of an entity instance.
	 * @param expectedType The exact expected type of the entity instance.
	 * @param identifier The entity identifier.
	 * @return An ordinal to pass later to {@link #retrieve(PojoLoadingTypeContext, int)}.
	 * @see #loadBlocking(Deadline)
	 */
	int planLoading(PojoLoadingTypeContext<? extends T> expectedType, Object identifier);

	/**
	 * Loads the entities whose identifiers were passed to {@link #planLoading(PojoLoadingTypeContext, Object)},
	 * blocking the current thread while doing so.
	 * @param deadline The deadline for loading the entities, or null if there is no deadline.
	 */
	void loadBlocking(Deadline deadline);

	/**
	 * Retrieves a loaded entity instance.
	 * @param <T2> The exact expected type for the entity instance.
	 * @param expectedType The expected type for the entity instance.
	 * Must be the same type passed to {@link #planLoading(PojoLoadingTypeContext, Object)}.
	 * @param ordinal The ordinal returned by {@link #planLoading(PojoLoadingTypeContext, Object)}.
	 * @return The loaded entity instance, or {@code null} if it was not found.
	 * The instance is guaranteed to be an instance of the given type <strong>exactly</strong> (not a subtype).
	 */
	<T2 extends T> T2 retrieve(PojoLoadingTypeContext<T2> expectedType, int ordinal);

	void clear();
}
