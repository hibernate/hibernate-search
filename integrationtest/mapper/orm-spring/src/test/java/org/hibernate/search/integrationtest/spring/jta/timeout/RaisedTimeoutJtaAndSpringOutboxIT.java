/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.integrationtest.spring.jta.JtaAndSpringOutboxApplicationConfiguration;
import org.hibernate.search.integrationtest.spring.jta.dao.SnertDAO;
import org.hibernate.search.integrationtest.spring.jta.entity.Snert;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atomikos.icatch.jta.TransactionManagerImp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JtaAndSpringOutboxApplicationConfiguration.class)
@ActiveProfiles({ "jta", "outbox", "transaction-timeout", "raised-timeout" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RaisedTimeoutJtaAndSpringOutboxIT {

	@Autowired
	@Rule
	public BackendMock backendMock;

	@Autowired
	private SnertDAO snertDAO;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Before
	public void checkJta() {
		assertThat( entityManagerFactory.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry().getService( TransactionCoordinatorBuilder.class ) )
				.returns( true, TransactionCoordinatorBuilder::isJta );

		// We changed the default Atomikos timeout to 1s (1000ms)
		assertThat( TransactionManagerImp.getDefaultTimeout() ).isOne();

		// However, the test is not supposed to time out,
		// because we raised the timeout to 60 seconds in Hibernate Search properties
	}

	@Test
	public void test() throws Exception {
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
