/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.impl;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copied from Hibernate Core's org.hibernate.util.LRUMap
 * A simple LRU cache that implements the <code>Map</code> interface. Instances
 * are not thread-safe and should be synchronized externally, for instance by
 * using {@link java.util.Collections#synchronizedMap}.
 *
 * @author Manuel Dominguez Sarmiento
 */
public class LRUMap extends LinkedHashMap implements Serializable {

	private static final long serialVersionUID = -2613234214057068628L;

	private final int maxEntries;

	public LRUMap(int maxEntries) {
		super( maxEntries, .75f, true );
		this.maxEntries = maxEntries;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return ( size() > maxEntries );
	}
}
