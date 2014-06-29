/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
	 * @param initialSize for tuning of the initial size of the Map
	 * @param concurrencyLevel the estimated number of concurrently
	 * updating threads. The implementation performs internal sizing
	 * to try to accommodate this many threads.
	 * @return a new concurrent map with the properties described above.
	 */
	public static <K,V> ConcurrentMap<K,V> createIdentityWeakKeyConcurrentMap(int initialSize, int concurrencyLevel) {
		return new ConcurrentReferenceHashMap(
				initialSize, 0.75f, concurrencyLevel,
				ReferenceType.WEAK, ReferenceType.STRONG,
				EnumSet.of( Option.IDENTITY_COMPARISONS ) );
	}

}
