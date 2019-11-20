/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

public interface HibernateOrmMassIndexingIndexedTypeContext<E> {

	/**
	 * @return The entity type from the Hibernate ORM metamodel.
	 */
	EntityTypeDescriptor<E> getEntityType();

}
