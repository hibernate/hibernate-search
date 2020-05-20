/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public interface HibernateOrmMassIndexingIndexedTypeContext<E> {

	PojoRawTypeIdentifier<E> typeIdentifier();

	/**
	 * @return The name of the entity in the JPA metamodel.
	 */
	String jpaEntityName();

	/**
	 * @return The Hibernate ORM entity persister.
	 */
	EntityPersister entityPersister();

	/**
	 * @return A representation of the entity type in the Hibernate ORM metamodel.
	 * @throws org.hibernate.search.util.common.SearchException If there isn't any representation of the entity type
	 * in the Hibernate ORM metamodel.
	 * Typically, dynamic-map entities do not have a representation in the Hibernate ORM metamodel
	 * (which prevents any operation relying on JPA Criteria, in particular).
	 */
	EntityTypeDescriptor<E> entityTypeDescriptor();

}
