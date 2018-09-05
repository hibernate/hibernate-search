/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.impl;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.util.impl.ConcurrentReferenceHashMap.Option;
import org.hibernate.search.util.impl.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Helper class to create maps with commonly needed constructors.
 *
 * @author Sanne Grinovero
 */
public class Maps {

	private Maps() {
		//not to be constructed
	}

	/**
	 * Creates a ConcurrentMap using weak references to keys, so that garbage collection
	 * of the key allows to remove the value from the map.
	 * Comparison on the keys is based on identity reference.
	 *
	 * @param <K> the key type in the map
	 * @param <V> the value type in the map
	 * @param initialSize for tuning of the initial size of the Map
	 * @param concurrencyLevel the estimated number of concurrently
	 * updating threads. The implementation performs internal sizing
	 * to try to accommodate this many threads.
	 * @return a new concurrent map with the properties described above.
	 */
	public static <K,V> ConcurrentMap<K,V> createIdentityWeakKeyConcurrentMap(int initialSize, int concurrencyLevel) {
		return new ConcurrentReferenceHashMap<>(
				initialSize, 0.75f, concurrencyLevel,
				ReferenceType.WEAK, ReferenceType.STRONG,
				EnumSet.of( Option.IDENTITY_COMPARISONS ) );
	}

}
