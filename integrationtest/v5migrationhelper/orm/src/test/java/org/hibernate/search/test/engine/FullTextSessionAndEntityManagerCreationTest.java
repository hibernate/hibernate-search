/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.hibernate.search.Search;

import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class FullTextSessionAndEntityManagerCreationTest {

	@Test
	public void testCreatingFullTextSessionByPassingNullFails() throws Exception {
		try {
			Search.getFullTextSession( null );
			fail( "A valid session must be passed" );
		}
		catch (IllegalArgumentException e) {
			assertEquals(
					"Unexpected error code: " + e.getMessage(),
					"HSEARCH000178: Unable to create a FullTextSession from a null Session",
					e.getMessage() );
		}
	}

	@Test
	public void testCreatingFullEntityManagerByPassingNullFails() throws Exception {
		try {
			org.hibernate.search.jpa.Search.getFullTextEntityManager( null );
			fail( "A valid session must be passed" );
		}
		catch (IllegalArgumentException e) {
			assertEquals(
					"Unexpected error code: " + e.getMessage(),
					"HSEARCH000179: Unable to create a FullTextEntityManager from a null EntityManager",
					e.getMessage() );
		}
	}

}
