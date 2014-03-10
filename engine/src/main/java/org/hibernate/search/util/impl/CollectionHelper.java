/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.util.impl;

import java.util.ArrayList;
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

}
