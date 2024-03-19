/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
