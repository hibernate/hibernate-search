/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.orm.common.EntityReference;

/**
 * An {@link EntityLoader} that can be easily composed with others entity loaders.
 * <p>
 * See {@link HibernateOrmByTypeEntityLoader} for uses.
 *
 * @param <E> The type of loaded entities.
 */
public interface HibernateOrmComposableEntityLoader<E> extends EntityLoader<EntityReference, E> {

	@Override
	default List<E> loadBlocking(List<EntityReference> references) {
		// Load all references
		Map<EntityReference, E> objectsByReference = new HashMap<>();
		loadBlocking( references, objectsByReference );

		// Re-create the list of objects in the same order
		List<E> result = new ArrayList<>( references.size() );
		for ( EntityReference reference : references ) {
			result.add( objectsByReference.get( reference ) );
		}
		return result;
	}

	/**
	 * For each reference in the given list,
	 * loads the corresponding object and puts it as a value in the given map,
	 * blocking the current thread while doing so.
	 * <p>
	 * When an object cannot be found, the map is not altered.
	 *
	 * @param references A list of references to the objects to load.
	 * @param entitiesByReference A map with references as keys and loaded entities as values.
	 * Initial values are undefined and the loader must not rely on them.
	 */
	void loadBlocking(List<EntityReference> references, Map<? super EntityReference, ? super E> entitiesByReference);

}
