/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides some methods for simplified collection instantiation.
 *
 * @author Gunnar Morling
 * @author Hardy Ferentschik
 */
public final class CollectionHelper {

	private CollectionHelper() {
		// not allowed
	}

	public static <K, V> HashMap<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> HashMap<K, V> newHashMap(int size) {
		return new HashMap<K, V>( size );
	}

	public static <K, V> SortedMap<K, V> newSortedMap() {
		return new TreeMap<K, V>();
	}

	public static <T> HashSet<T> newHashSet() {
		return new HashSet<T>();
	}

	public static <T> ArrayList<T> newArrayList() {
		return new ArrayList<T>();
	}

	public static <T> ArrayList<T> newArrayList(final int size) {
		return new ArrayList<T>( size );
	}

	public static <T> Set<T> asSet(T... ts) {
		HashSet<T> set = new HashSet<T>( ts.length );
		Collections.addAll( set, ts );
		return set;
	}

	public static <T> List<T> toImmutableList(final Collection<T> c) {
		if ( c.isEmpty() ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( new ArrayList<T>( c ) );
		}
	}

	public static Set<String> asImmutableSet(String[] names) {
		//The intention here is to save some memory by picking the simplest safe representation,
		// as we usually require immutable sets for long living metadata:
		if ( names == null || names.length == 0 ) {
			return Collections.<String>emptySet();
		}
		else if ( names.length == 1 ) {
			return Collections.singleton( names[0] );
		}
		else {
			HashSet<String> hashSet = new HashSet<>( Arrays.asList( names ) );
			return Collections.unmodifiableSet( hashSet );
		}
	}

}
