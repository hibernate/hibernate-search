/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import javax.persistence.EntityManager;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;

public interface HibernateOrmScopeMappingContext
		extends BackendMappingContext, HibernateOrmMassIndexingMappingContext {

	@Override
	HibernateOrmScopeSessionContext sessionContext(EntityManager entityManager);

}
