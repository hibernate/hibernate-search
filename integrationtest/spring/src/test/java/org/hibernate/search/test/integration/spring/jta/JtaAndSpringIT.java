/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.jta;

import javax.inject.Inject;

import org.hibernate.search.test.integration.spring.jta.dao.SnertDAO;
import org.hibernate.search.test.integration.spring.jta.entity.Snert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JtaAndSpringApplicationConfiguration.class)
@ActiveProfiles("jta")
public class JtaAndSpringIT {

	@Inject
	private SnertDAO snertDAO;

	@Test
	public void test() {
		Snert snert = new Snert();
		snert.setName( "dave" );
		snert.setNickname( "dude" );
		snert.setAge( 99 );
		snert.setCool( Boolean.TRUE );
		snertDAO.persist( snert );
	}
}
