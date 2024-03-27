/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope.impl;

import jakarta.persistence.EntityManager;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;

public interface HibernateOrmScopeMappingContext
		extends BackendMappingContext, HibernateOrmMassIndexingMappingContext {

	@Override
	HibernateOrmScopeSessionContext sessionContext(EntityManager entityManager);

}
