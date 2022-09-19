/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the creation of a {@link HibernateOrmSearchSession}
 * using a {@link HibernateSearchContextProviderService}
 * with an {@link EntityManager} owned by a different {@link SessionFactory}.
 */
public class DifferentSessionFactoriesIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;
	private SessionFactory sessionFactoryAlt;

	@Before
	public void setup() {
		sessionFactory = initABasicSessionFactory();
		sessionFactoryAlt = initABasicSessionFactory();
	}

	@Test
	public void tryToUseDifferentSessionFactories() {
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
