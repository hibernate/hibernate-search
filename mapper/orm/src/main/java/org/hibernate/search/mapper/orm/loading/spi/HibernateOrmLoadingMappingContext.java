/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.spi;

import jakarta.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

public interface HibernateOrmLoadingMappingContext {

	EntityLoadingCacheLookupStrategy cacheLookupStrategy();

	int fetchSize();

	SessionFactoryImplementor sessionFactory();

	HibernateOrmLoadingSessionContext sessionContext(EntityManager entityManager);

}
