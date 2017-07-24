/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi.impl;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.spi.IndexedTypeMap;

public final class IndexedTypeMaps {

	private IndexedTypeMaps() {
		//Utility class: not to be constructed
	}

	@SuppressWarnings("unchecked") // The returned map will work for any V
	public static <V> IndexedTypeMap<V> empty() {
		return DelegatingIndexedTypeMap.EMPTY;
	}

	public static <V> IndexedTypeMap<V> hashMap() {
		return new DelegatingIndexedTypeMap<>( new HashMap<>(), new HashMap<>() );
	}

	public static <V> IndexedTypeMap<V> concurrentHashMap() {
		return new DelegatingIndexedTypeMap<>( new ConcurrentHashMap<>(), new ConcurrentHashMap<>() );
	}


}
