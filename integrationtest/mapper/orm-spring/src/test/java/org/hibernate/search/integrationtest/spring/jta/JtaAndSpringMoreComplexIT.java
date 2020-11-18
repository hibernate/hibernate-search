/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.integrationtest.spring.jta.dao.BoxDAO;
import org.hibernate.search.integrationtest.spring.jta.entity.Box;
import org.hibernate.search.integrationtest.spring.jta.entity.Doughnut;
import org.hibernate.search.integrationtest.spring.jta.entity.Muffin;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JtaAndSpringApplicationConfiguration.class)
@ActiveProfiles("jta")
@PortedFromSearch5(original = "org.hibernate.search.test.integration.spring.jta.JtaAndSpringMoreComplexIT")
public class JtaAndSpringMoreComplexIT {

	@Autowired
	@Rule
	public BackendMock backendMock;

	@Autowired
	private BoxDAO boxDAO;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Before
	public void checkJta() {
		assertThat( entityManagerFactory.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry().getService( TransactionCoordinatorBuilder.class ) )
				.returns( true, TransactionCoordinatorBuilder::isJta );
	}

	@Test
	public void testMuffins() {
		Box box = new Box();
		box.setContainerId( 1L );
		box.setColor( "red-and-white" );

		backendMock.expectWorks( "Box" )
				.add( "1", b -> { } )
				.processedThenExecuted();
		boxDAO.persist( box );
		backendMock.verifyExpectationsMet();

		Muffin muffin = new Muffin();
		muffin.setMuffinId( 1L );
		muffin.setKind( "blueberry" );
		muffin.setBox( box );

		box.addMuffin( muffin );

		boxDAO.merge( box );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void testDoughnuts() {
		Box box = new Box();
		box.setContainerId( 2L );
		box.setColor( "red-and-white" );

		backendMock.expectWorks( "Box" )
				.add( "2", b -> { } )
				.processedThenExecuted();
		boxDAO.persist( box );
		backendMock.verifyExpectationsMet();

		Doughnut doughnut = new Doughnut();
		doughnut.setDoughnutId( 2L );
		doughnut.setKind( "glazed" );
		doughnut.setBox( box );

		box.addDoughnut( doughnut );

		backendMock.expectWorks( "Doughnut" )
				.add( "2", b -> { } )
				.processedThenExecuted();
		boxDAO.merge( box );
		backendMock.verifyExpectationsMet();
	}
}
