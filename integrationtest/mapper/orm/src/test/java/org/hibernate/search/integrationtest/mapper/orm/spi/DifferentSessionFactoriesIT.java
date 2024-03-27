/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.spi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextProviderService;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests the creation of a {@link HibernateOrmSearchSession}
 * using a {@link HibernateSearchContextProviderService}
 * with an {@link EntityManager} owned by a different {@link SessionFactory}.
 */
class DifferentSessionFactoriesIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;
	private SessionFactory sessionFactoryAlt;

	@BeforeEach
	void setup() {
		sessionFactory = initABasicSessionFactory();
		sessionFactoryAlt = initABasicSessionFactory();
	}

	@Test
	void tryToUseDifferentSessionFactories() {
		// mapping is taken from the alternative session factory
		HibernateSearchContextProviderService contextProvider =
				sessionFactoryAlt.unwrap( SessionFactoryImplementor.class )
						.getServiceRegistry().getService( HibernateSearchContextProviderService.class );

		// try to use an entityManager owned by the original session factory instead
		assertThatThrownBy( () -> with( sessionFactory ).runNoTransaction(
				session -> HibernateOrmSearchSession.get( contextProvider.get(), (SessionImplementor) session )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Unable to create a SearchSession for sessions created using a different session factory."
				);
	}

	private SessionFactory initABasicSessionFactory() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );

		SessionFactory factory = ormSetupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
		return factory;
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
