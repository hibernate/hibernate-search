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
