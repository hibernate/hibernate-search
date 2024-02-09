/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.spi;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface HibernateOrmLoadingTypeContext<E> {

	/**
	 * @return The name of the entity in the JPA metamodel.
	 */
	String jpaEntityName();

	PojoRawTypeIdentifier<E> typeIdentifier();

	PojoLoadingTypeContext<E> delegate();

	/**
	 * @return The entity mapping type, giving access to a representation of the entity type in the Hibernate ORM metamodel.
	 */
	EntityMappingType entityMappingType();

	HibernateOrmEntityLoadingStrategy<? super E, ?> loadingStrategy();

}
