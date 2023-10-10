/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.Search;

import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
class FullTextSessionAndEntityManagerCreationTest {

	@Test
	void testCreatingFullTextSessionByPassingNullFails() {
		assertThatThrownBy( () -> Search.getFullTextSession( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"HSEARCH000178: Unable to create a FullTextSession from a null Session"
				);
	}

	@Test
	void testCreatingFullEntityManagerByPassingNullFails() {
		assertThatThrownBy( () -> org.hibernate.search.jpa.Search.getFullTextEntityManager( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"HSEARCH000179: Unable to create a FullTextEntityManager from a null EntityManager"
				);
	}

}
