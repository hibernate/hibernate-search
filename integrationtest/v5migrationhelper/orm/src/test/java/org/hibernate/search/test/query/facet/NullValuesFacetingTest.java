/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.facet;

import org.hibernate.Session;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

class NullValuesFacetingTest extends AbstractFacetTest {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1917")
	void testNullStringFaceting() {
		Car car = new Car( null, "yellow", 2500 );
		fullTextSession.save( car );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1917")
	void testEmptyStringFaceting() {
		Car car = new Car( "", "red", 2500 );
		fullTextSession.save( car );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1917")
	void testNullNumericFaceting() {
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
