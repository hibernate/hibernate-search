/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import org.hibernate.persister.entity.EntityPersister;

public interface HibernateOrmLoadingIndexedTypeContext {

	/**
	 * @return The name of the entity in the JPA metamodel.
	 */
	String getJpaEntityName();

	/**
	 * @return The entity persister, giving access to a representation of the entity type in the Hibernate ORM metamodel.
	 */
	EntityPersister getEntityPersister();

	EntityLoaderFactory getLoaderFactory();

}
