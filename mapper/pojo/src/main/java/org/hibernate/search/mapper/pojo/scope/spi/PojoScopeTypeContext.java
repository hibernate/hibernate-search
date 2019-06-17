/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;

/**
 * @param <E> The entity type.
 */
public interface PojoScopeTypeContext<E> {

	/**
	 * @return The entity type as a {@link Class}.
	 */
	Class<E> getJavaClass();

	/**
	 * @return The metadata for the type.
	 */
	PojoMappingTypeMetadata getMappingMetadata();

}
