/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
