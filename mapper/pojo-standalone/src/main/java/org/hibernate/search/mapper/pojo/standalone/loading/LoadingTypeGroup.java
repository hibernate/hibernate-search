/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import java.util.Map;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A group of entity types for entity loading.
 *
 * @param <E> The type of loaded entities.
 */
@Incubating
public interface LoadingTypeGroup<E> {

	/**
	 * @return A map of entity classes by entity name,
	 * representing all entity types included in this group, and only these types.
	 */
	Map<String, Class<? extends E>> includedTypesMap();

	/**
	 * @param entity An entity of any type.
	 * @return {@code true} if the given entity instance is included in this type group.
	 */
	boolean includesInstance(Object entity);

}
