/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;

public interface HibernateOrmMassIndexingMappingContext extends PojoMassIndexingMappingContext {

	SessionFactoryImplementor sessionFactory();

	HibernateOrmScopeSessionContext sessionContext(EntityManager entityManager);

	HibernateOrmSessionTypeContextProvider typeContextProvider();
}
