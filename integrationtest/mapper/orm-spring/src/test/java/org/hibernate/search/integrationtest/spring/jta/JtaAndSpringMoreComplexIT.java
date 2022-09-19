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
import org.hibernate.search.integrationtest.spring.jta.dao.BoxDAO;
import org.hibernate.search.integrationtest.spring.jta.entity.Box;
import org.hibernate.search.integrationtest.spring.jta.entity.Doughnut;
import org.hibernate.search.integrationtest.spring.jta.entity.Muffin;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSessionHolder;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JtaAndSpringApplicationConfiguration.class)
@ActiveProfiles("jta")
@PortedFromSearch5(original = "org.hibernate.search.test.integration.spring.jta.JtaAndSpringMoreComplexIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

	@After
	public void checkNoMemoryLeak() {
		assertThat( HibernateOrmSearchSessionHolder.staticMapSize() ).isZero();
	}

	@Test
	public void testMuffins() {
		Box box = new Box();
		box.setContainerId( 1L );
		box.setColor( "red-and-white" );

		backendMock.expectWorks( "Box" )
				.add( "1", b -> b
						.field( "color", "red-and-white" ) );
		boxDAO.persist( box );
		backendMock.verifyExpectationsMet();

		Muffin muffin = new Muffin();
		muffin.setMuffinId( 1L );
		muffin.setKind( "blueberry" );
		muffin.setBox( box );

		box.addMuffin( muffin );

		backendMock.expectWorks( "Box" )
				.addOrUpdate( "1", b -> b
						.field( "color", "red-and-white" )
						.objectField( "muffinSet", b2 -> b2
								.field( "kind", "blueberry" ) ) );
		boxDAO.merge( box );
		backendMock.verifyExpectationsMet();

		// Test lazy-loading a set of muffins
		backendMock.expectWorks( "Box" )
				.addOrUpdate( "1", b -> b
						.field( "color", "blue" )
						.objectField( "muffinSet", b2 -> b2
								.field( "kind", "blueberry" ) ) );
		boxDAO.changeColor( box.getContainerId(), "blue" );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void testDoughnuts() {
		Box box = new Box();
		box.setContainerId( 2L );
		box.setColor( "red-and-white" );

		backendMock.expectWorks( "Box" )
				.add( "2", b -> b
						.field( "color", "red-and-white" ) );
		boxDAO.persist( box );
		backendMock.verifyExpectationsMet();

		Doughnut doughnut = new Doughnut();
		doughnut.setDoughnutId( 2L );
		doughnut.setKind( "glazed" );
		doughnut.setBox( box );

		box.addDoughnut( doughnut );

		backendMock.expectWorks( "Doughnut" )
				.add( "2", b -> b.field( "kind", "glazed" ) );
		backendMock.expectWorks( "Box" )
				.addOrUpdate( "2", b -> b
						.field( "color", "red-and-white" )
						.objectField( "doughnutSet", b2 -> b2
								.field( "kind", "glazed" ) ) );
		boxDAO.merge( box );
		backendMock.verifyExpectationsMet();

		// Test lazy-loading a set of doughnuts
		backendMock.expectWorks( "Box" )
				.addOrUpdate( "2", b -> b
						.field( "color", "blue" )
						.objectField( "doughnutSet", b2 -> b2
								.field( "kind", "glazed" ) ) );
		boxDAO.changeColor( box.getContainerId(), "blue" );
		backendMock.verifyExpectationsMet();
	}
}
