/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.loader;

import java.util.Map;

/**
 * A group entity types for entity loading entities during mass indexing.
 * @param <E> The resulting entity type (output)
 */
public interface MassIndexingEntityLoadingTypeGroup<E> {

	/**
	 * @return The names of all entity types included in this group.
	 */
	Map<String, Class<? extends E>> includedEntityMap();

	/**
	 * @param entity An entity of any type.
	 * @return {@code true} if the given entity instance is included in this type group.
	 */
	boolean includesInstance(Object entity);

}
