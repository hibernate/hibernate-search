/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.spi;

import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManager;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

public interface HibernateOrmMapping {

	HibernateOrmSearchManager createSearchManager(EntityManager entityManager);

	HibernateOrmSearchManagerBuilder createSearchManagerWithOptions(EntityManager entityManager);

	/**
	 * @param type A Java type.
	 * @return {@code true} if this type can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code false} if it cannot.
	 * Workable types include both indexable types and contained entity types.
	 */
	boolean isWorkable(Class<?> type);

	/**
	 * @param type A Java type.
	 * @return {@code true} if this type is indexable, {@code false} if it is not.
	 */
	boolean isIndexable(Class<?> type);

	/**
	 * @param type A Java type.
	 * @return {@code true} if this type is searchable
	 * (i.e. it can be passed to {@link org.hibernate.search.mapper.orm.jpa.FullTextEntityManager#search(Class)}),
	 * {@code false} if it is not.
	 */
	boolean isSearchable(Class<?> type);

	/**
	 * @param entity An entity.
	 * @return {@code true} if this entity can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code false} if it cannot.
	 */
	boolean isWorkable(Object entity);

	/**
	 * Given a set of target entity, return the set of configured subtypes that are indexed.
	 *
	 * @param entityType the target set
	 * @return the set of configured subtypes that are indexed
	 */
	<E> Set<Class<? extends E>> getIndexedTypesPolymorphic(Class<E> entityType);

}
