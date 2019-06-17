/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;

public interface HibernateOrmLoadingIndexedTypeContext<E> {

	/**
	 * @return The entity type as a {@link Class}.
	 */
	Class<E> getJavaClass();

	// FIXME HSEARCH-3203 Replace this temporary solution with an object-oriented one.
	PojoMappingTypeMetadata getMappingMetadata();

}
