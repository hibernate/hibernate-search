/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.integrationtest.spring.extension.HibernateSpringPropertiesSetterExtension;
import org.hibernate.search.integrationtest.spring.jta.dao.SnertDAO;
import org.hibernate.search.integrationtest.spring.jta.entity.Snert;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSessionHolder;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(HibernateSpringPropertiesSetterExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = JtaAndSpringApplicationConfiguration.class)
@ActiveProfiles("jta")
@PortedFromSearch5(original = "org.hibernate.search.test.integration.spring.jta.JtaAndSpringIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JtaAndSpringIT {

	@Autowired
	@RegisterExtension
	public BackendMock backendMock;

	@Autowired
	private SnertDAO snertDAO;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void checkJta() {
		assertThat( entityManagerFactory.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry().getService( TransactionCoordinatorBuilder.class ) )
				.returns( true, TransactionCoordinatorBuilder::isJta );
	}

	@AfterEach
	void checkNoMemoryLeak() {
		assertThat( HibernateOrmSearchSessionHolder.staticMapSize() ).isZero();
	}

	@Test
	void test() {
		Snert snert = new Snert();
		snert.setId( 1L );
		snert.setName( "dave" );
		snert.setNickname( "dude" );
		snert.setAge( 99 );
		snert.setCool( Boolean.TRUE );

		backendMock.expectWorks( "Snert" )
				.add( "1", b -> b
						.field( "age", 99 )
						.field( "cool", true )
						.field( "name", "dave" ) );
		snertDAO.persist( snert );
		backendMock.verifyExpectationsMet();
	}
}
