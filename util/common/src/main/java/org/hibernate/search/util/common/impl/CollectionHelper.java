/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Provides some methods for simplified collection instantiation.
 *
 * @author Gunnar Morling
 * @author Hardy Ferentschik
 * @author Guillaume Smet
 */
public final class CollectionHelper {

	private CollectionHelper() {
	}

	public static <K, V> HashMap<K, V> newHashMap(int size) {
		return new HashMap<>( getInitialCapacityFromExpectedSize( size ) );
	}

	public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int size) {
		return new LinkedHashMap<>( getInitialCapacityFromExpectedSize( size ) );
	}

	public static <T> HashSet<T> newHashSet(int size) {
		return new HashSet<>( getInitialCapacityFromExpectedSize( size ) );
	}

	public static <T> LinkedHashSet<T> newLinkedHashSet(int size) {
		return new LinkedHashSet<>( getInitialCapacityFromExpectedSize( size ) );
	}

	@SafeVarargs
	public static <T> Set<T> asSet(T... ts) {
		Set<T> set = new HashSet<>( getInitialCapacityFromExpectedSize( ts.length ) );
		Collections.addAll( set, ts );
		return set;
	}

	@SafeVarargs
	public static <T> Set<T> asLinkedHashSet(T... ts) {
		Set<T> set = new LinkedHashSet<>( getInitialCapacityFromExpectedSize( ts.length ) );
		Collections.addAll( set, ts );
		return set;
	}

	@SafeVarargs
	public static <T> List<T> asList(T firstItem, T... otherItems) {
		List<T> list = new ArrayList<>( otherItems.length + 1 );
		list.add( firstItem );
		Collections.addAll( list, otherItems );
		return list;
	}

	@SafeVarargs
	public static <T> List<T> asImmutableList(T ... items) {
		switch ( items.length ) {
			case 0:
				return Collections.emptyList();
			case 1:
				return Collections.singletonList( items[0] );
			default:
				return Collections.unmodifiableList( Arrays.asList( items ) );
		}
	}

	public static <T> List<T> toImmutableList(List<? extends T> list) {
		switch ( list.size() ) {
			case 0:
				return Collections.emptyList();
			case 1:
				return Collections.singletonList( list.get( 0 ) );
			default:
				return Collections.unmodifiableList( list );
		}
	}

	public static <T> Set<T> toImmutableSet(Set<? extends T> set) {
		switch ( set.size() ) {
			case 0:
				return Collections.emptySet();
			case 1:
				return Collections.singleton( set.iterator().next() );
			default:
				return Collections.unmodifiableSet( set );
		}
	}

	public static <K, V> Map<K, V> toImmutableMap(Map<K, V> map) {
		switch ( map.size() ) {
			case 0:
				return Collections.emptyMap();
			case 1:
				Entry<K, V> entry = map.entrySet().iterator().next();
				return Collections.singletonMap( entry.getKey(), entry.getValue() );
			default:
				return Collections.unmodifiableMap( map );
		}
	}

	/**
	 * As the default loadFactor is of 0.75, we need to calculate the initial capacity from the expected size to avoid
	 * resizing the collection when we populate the collection with all the initial elements. We use a calculation
	 * similar to what is done in {@link HashMap#putAll(Map)}.
	 *
	 * @param expectedSize the expected size of the collection
	 * @return the initial capacity of the collection
	 */
	private static int getInitialCapacityFromExpectedSize(int expectedSize) {
		if ( expectedSize < 3 ) {
			return expectedSize + 1;
		}
		return (int) ( (float) expectedSize / 0.75f + 1.0f );
	}
}
