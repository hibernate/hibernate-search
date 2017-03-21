/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.jta;

import javax.inject.Inject;

import org.hibernate.search.test.integration.spring.jta.dao.BoxDAO;
import org.hibernate.search.test.integration.spring.jta.entity.Box;
import org.hibernate.search.test.integration.spring.jta.entity.Doughnut;
import org.hibernate.search.test.integration.spring.jta.entity.Muffin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JtaAndSpringApplicationConfiguration.class)
@ActiveProfiles("jta")
public class JtaAndSpringMoreComplexIT {
	@Inject
	private BoxDAO boxDAO;

	@Test
	public void testMuffins() throws Exception {
		Box box = new Box();
		box.setColor( "red-and-white" );
		boxDAO.persist( box );

		Muffin muffin = new Muffin();
		muffin.setKind( "blueberry" );
		muffin.setBox( box );

		box.addMuffin( muffin );

		boxDAO.merge( box );
	}

	@Test
	public void testDoughnuts() throws Exception {
		Box box = new Box();
		box.setColor( "red-and-white" );
		boxDAO.persist( box );

		Doughnut doughnut = new Doughnut();
		doughnut.setKind( "glazed" );
		doughnut.setBox( box );

		box.addDoughnut( doughnut );

		boxDAO.merge( box );
	}
}
