/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextProviderService;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.impl.DelegatingSearchSession;

public final class Search {

	private Search() {
		// Private constructor
	}

	/**
	 * Retrieve the {@link SearchMapping} from a Hibernate ORM {@link SessionFactory}.
	 *
	 * @param sessionFactory A Hibernate ORM session factory.
	 * @return The corresponding {@link SearchSession}.
	 * @throws org.hibernate.search.util.common.SearchException if the session NOT {@link Session#isOpen()}.
	 */
	public static SearchMapping mapping(SessionFactory sessionFactory) {
		SessionFactoryImplementor sessionFactoryImpl =
				HibernateOrmUtils.toSessionFactoryImplementor( sessionFactory );
		return getSearchMapping( sessionFactoryImpl );
	}

	/**
	 * Retrieve the {@link SearchMapping} from a JPA {@link EntityManagerFactory}.
	 *
	 * @param entityManagerFactory A JPA entity manager factory.
	 * @return The corresponding {@link SearchSession}.
	 * @throws org.hibernate.search.util.common.SearchException if the session NOT {@link Session#isOpen()}.
	 */
	public static SearchMapping mapping(EntityManagerFactory entityManagerFactory) {
		SessionFactoryImplementor sessionFactoryImpl =
				HibernateOrmUtils.toSessionFactoryImplementor( entityManagerFactory );
		return getSearchMapping( sessionFactoryImpl );
	}

	/**
	 * Retrieve the {@link SearchSession} from a Hibernate ORM {@link Session}.
	 * <p>
	 * The resulting instance depends on the passed {@link Session}:
	 * closing the {@link Session} will close the {@link SearchSession}.
	 * The {@link SearchSession} will share the {@link Session}'s persistence context.
	 *
	 * @param session A Hibernate ORM session.
	 * @return The corresponding {@link SearchSession}.
	 * @throws org.hibernate.search.util.common.SearchException if the session NOT {@link Session#isOpen()}.
	 */
	public static SearchSession session(Session session) {
		return createSearchSession( session );
	}

	/**
	 * Retrieve the {@link SearchSession} from a JPA {@link EntityManager}.
	 * <p>
	 * The resulting instance depends on the passed {@link EntityManager}:
	 * closing the {@link EntityManager} will close the {@link SearchSession}.
	 * The {@link SearchSession} will share the {@link EntityManager}'s persistence context.
	 *
	 * @param entityManager A JPA entity manager.
	 * @return The corresponding {@link SearchSession}.
	 * @throws org.hibernate.search.util.common.SearchException if the entity manager NOT {@link EntityManager#isOpen()}.
	 */
	public static SearchSession session(EntityManager entityManager) {
		Session session = HibernateOrmUtils.toSession( entityManager );
		return createSearchSession( session );
	}

	private static SearchMapping getSearchMapping(SessionFactoryImplementor sessionFactoryImplementor) {
		HibernateSearchContextProviderService mappingContextProvider =
				HibernateSearchContextProviderService.get( sessionFactoryImplementor );
		return mappingContextProvider.get();
	}

	private static SearchSession createSearchSession(Session session) {
		HibernateSearchContextProviderService mappingContextProvider = HibernateSearchContextProviderService.get(
				HibernateOrmUtils.toSessionFactoryImplementor( session.getSessionFactory() ) );
		return new DelegatingSearchSession( mappingContextProvider, session );
	}

}
