/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class LinkedNodeTest {

	@Test
	void testToString() {
		assertThat( LinkedNode.of( 42 ).toString() )
				.isEqualTo( "[42]" );
		assertThat( LinkedNode.of( 42, 1, 2, 3 ).toString() )
				.isEqualTo( "[42 => 1 => 2 => 3]" );
	}

	@Test
	void testEqualsAndHashCode() {
		assertThat( LinkedNode.of( 42 ) )
				.isEqualTo( LinkedNode.of( 42 ) );
		assertThat( LinkedNode.of( 42 ).hashCode() )
				.isEqualTo( LinkedNode.of( 42 ).hashCode() );

		assertThat( LinkedNode.of( 42, 1, 2, 3 ).hashCode() )
				.isEqualTo( LinkedNode.of( 42, 1, 2, 3 ).hashCode() );
		assertThat( LinkedNode.of( 42, 1, 2, 3 ).hashCode() )
				.isEqualTo( LinkedNode.of( 42, 1, 2, 3 ).hashCode() );

		assertThat( LinkedNode.of( 42 ) )
				.isNotEqualTo( LinkedNode.of( 43 ) );
		assertThat( LinkedNode.of( 42 ) )
				.isNotEqualTo( LinkedNode.of( 42, 1 ) );
		assertThat( LinkedNode.of( 42, 1, 2, 3 ) )
				.isNotEqualTo( LinkedNode.of( 42, 1, 32, 3 ) );
	}

	@Test
	void findAndReverse() {
		Predicate<Integer> is42 = Predicate.isEqual( 42 );

		// First matching is first element
		assertThat( LinkedNode.of( 42, 1, 2, 3 )
				.findAndReverse( is42 ) )
				.hasValue( LinkedNode.of( 42 ) );

		// First matching is somewhere in the middle
		assertThat( LinkedNode.of( 1, 42, 2, 3 )
				.findAndReverse( is42 ) )
				.hasValue( LinkedNode.of( 42, 1 ) );

		// First matching is last element
		assertThat( LinkedNode.of( 1, 2, 3, 42 )
				.findAndReverse( is42 ) )
				.hasValue( LinkedNode.of( 42, 3, 2, 1 ) );

		// No match
		assertThat( LinkedNode.of( 1, 2, 3 )
				.findAndReverse( is42 ) )
				.isEmpty();

		// Multiple matches
		assertThat( LinkedNode.of( 1, 42, 42, 2, 3, 42 )
				.findAndReverse( is42 ) )
				.hasValue( LinkedNode.of( 42, 1 ) );

		// Matching singleton
		assertThat( LinkedNode.of( 42 )
				.findAndReverse( is42 ) )
				.hasValue( LinkedNode.of( 42 ) );

		// Non-matching singleton
		assertThat( LinkedNode.of( 1 )
				.findAndReverse( is42 ) )
				.isEmpty();
	}

}
