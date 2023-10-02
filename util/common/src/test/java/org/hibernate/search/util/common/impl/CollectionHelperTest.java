/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class CollectionHelperTest {
	@Test
	void notAllElementsAreInSecondSet() {
		assertThat( CollectionHelper.isSubset(
				new TreeSet<>( List.of( 1, 2, 3, 4 ) ),
				new TreeSet<>( List.of( 1, 2, 3, 4 ) )
		) ).isTrue();

		assertThat( CollectionHelper.isSubset(
				new TreeSet<>( List.of( 1, 2, 4 ) ),
				new TreeSet<>( List.of( 1, 2, 3, 4 ) )
		) ).isTrue();

		assertThat( CollectionHelper.isSubset(
				new TreeSet<>( List.of( 4 ) ),
				new TreeSet<>( List.of( 1, 2, 3, 4 ) )
		) ).isTrue();

		assertThat( CollectionHelper.isSubset(
				new TreeSet<>( List.of( 1, 2, 4 ) ),
				new TreeSet<>( List.of( 1, 2, 3 ) )
		) ).isFalse();

		assertThat( CollectionHelper.isSubset(
				new TreeSet<>( List.of( 1, 2, 4, 6 ) ),
				new TreeSet<>( List.of( 1, 2, 3 ) )
		) ).isFalse();

		var set = new TreeSet<>( List.of( "4" ) );
		assertThat( CollectionHelper.isSubset( set, set ) ).isTrue();

		assertThat( CollectionHelper.isSubset(
				Set.of( 1, 2, 4 ),
				Set.of( 1, 2, 3, 4 )
		) ).isTrue();
		assertThat( CollectionHelper.isSubset(
				Set.of( 1, 2, 4 ),
				Set.of( 1, 2, 3 )
		) ).isFalse();
	}
}
