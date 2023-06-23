/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public class InsertionOrderTest {

	@Test
	public void stableGet() {
		InsertionOrder<MyKey> insertionOrder = new InsertionOrder<>();

		MyKey key1 = new MyKey( 42 );
		MyKey key2 = new MyKey( 1 );
		InsertionOrder.Key<MyKey> key1Wrapper = insertionOrder.wrapKey( key1 );
		InsertionOrder.Key<MyKey> key2Wrapper = insertionOrder.wrapKey( key2 );
		assertThat( key1Wrapper ).isLessThan( key2Wrapper );
		assertThat( key2Wrapper ).isGreaterThan( key1Wrapper );

		MyKey keyEqualTo1 = new MyKey( 42 );
		InsertionOrder.Key<MyKey> keyEqualTo1Wrapper = insertionOrder.wrapKey( keyEqualTo1 );
		assertThat( keyEqualTo1Wrapper ).isSameAs( key1Wrapper );
	}

	@Test
	public void concurrentSkipListMap() {
		InsertionOrder<MyKey> insertionOrder = new InsertionOrder<>();

		ConcurrentSkipListMap<InsertionOrder.Key<MyKey>, Integer> map = new ConcurrentSkipListMap<>();
		for ( int i = 0; i < 2000; i++ ) {
			MyKey key = new MyKey( i );
			map.put( insertionOrder.wrapKey( key ), i );

			assertThat( map.entrySet() )
					.extracting( e -> Map.entry( e.getKey().get(), e.getValue() ) )
					// Expect the map to contain entries MyKey(j) => j for j from 0 to i, in this exact order
					.containsExactlyElementsOf(
							IntStream.range( 0, i + 1 )
									.mapToObj( j -> Map.entry( new MyKey( j ), j ) )
									.collect( Collectors.toList() ) );
		}
	}

	private static class MyKey {
		final int i;

		private MyKey(int i) {
			this.i = i;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MyKey myKey = (MyKey) o;
			return i == myKey.i;
		}

		@Override
		public int hashCode() {
			return Objects.hash( i );
		}
	}

}
