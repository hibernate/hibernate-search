/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Predicate;

import org.junit.Test;

public class LinkedNodeTest {

	@Test
	public void testToString() {
		assertThat( LinkedNode.of( 42 ).toString() )
				.isEqualTo( "[42]" );
		assertThat( LinkedNode.of( 42, 1, 2, 3 ).toString() )
				.isEqualTo( "[42 => 1 => 2 => 3]" );
	}

	@Test
	public void testEqualsAndHashCode() {
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

}