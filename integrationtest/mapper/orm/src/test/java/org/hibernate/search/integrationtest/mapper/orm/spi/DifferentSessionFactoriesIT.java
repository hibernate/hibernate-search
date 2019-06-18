/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.spi;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests the creation of a {@link SearchSessionImplementor}, using a {@link HibernateOrmMapping} with an {@link EntityManager} owned by a different {@link SessionFactory}.
 */
public class DifferentSessionFactoriesIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SessionFactory sessionFactory;
	private SessionFactory sessionFactoryAlt;

	@Before
	public void setup() {
		sessionFactory = initABasicSessionFactory();
		sessionFactoryAlt = initABasicSessionFactory();
	}

	@Test
	public void tryToUseDifferentSessionFactories() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Mapping service cannot create a SearchSession using a different session factory." );

		// mapping is taken from the alternative session factory
		HibernateOrmMapping mapping = sessionFactoryAlt.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry().getService( HibernateSearchContextService.class ).getMapping();

		// try to use an entityManager owned by the original session factory instead
		OrmUtils.withinSession( sessionFactory, session -> {
			mapping.getSearchSession( (SessionImplementor) session );
		} );
	}

	private SessionFactory initABasicSessionFactory() {
		backendMock.expectAnySchema( IndexedEntity.INDEX );

		SessionFactory factory = ormSetupHelper.withBackendMock( backendMock )
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
