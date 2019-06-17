/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.spi;

import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContext;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionBuilder;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

public interface HibernateOrmMapping {

	SearchSessionImplementor createSession(EntityManager entityManager);

	SearchSessionBuilder createSessionWithOptions(EntityManager entityManager);

	/**
	 * @param type A Java type.
	 * @param <E> A Java type.
	 * @return The metadata for the given type if this type can be the subject of a work (i.e. it can be passed to
	 * {@link PojoWorkPlan#add(Object)} for instance), {@code null} otherwise.
	 */
	<E> HibernateOrmScopeTypeContext<E> getTypeContext(Class<E> type);

}
