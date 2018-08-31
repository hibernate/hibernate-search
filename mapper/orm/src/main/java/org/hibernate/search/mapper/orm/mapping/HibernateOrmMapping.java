/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import javax.persistence.EntityManager;

import org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;

public interface HibernateOrmMapping extends PojoMapping {

	HibernateOrmSearchManager createSearchManager(EntityManager entityManager);

	HibernateOrmSearchManagerBuilder createSearchManagerWithOptions(EntityManager entityManager);

	/**
	 * @param entity an entity
	 * @return {@code true} if this entity can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code false} if it cannot.
	 */
	boolean isWorkable(Object entity);

}
