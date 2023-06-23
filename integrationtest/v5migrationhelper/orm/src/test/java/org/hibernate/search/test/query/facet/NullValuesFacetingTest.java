/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import org.hibernate.Session;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

public class NullValuesFacetingTest extends AbstractFacetTest {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1917")
	public void testNullStringFaceting() throws Exception {
		Car car = new Car( null, "yellow", 2500 );
		fullTextSession.save( car );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1917")
	public void testEmptyStringFaceting() throws Exception {
		Car car = new Car( "", "red", 2500 );
		fullTextSession.save( car );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1917")
	public void testNullNumericFaceting() throws Exception {
		Car car = new Car( "honda", "yellow", null );
		fullTextSession.save( car );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class
		};
	}

	@Override
	public void loadTestData(Session session) {
		// noop
	}

}
