/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

/**
 * A fast, but cryptographically insecure hash function,
 * for use in hash tables.
 */
public class SimpleHashFunction {

	private SimpleHashFunction() {
	}

	public static <T> T pick(T[] content, String key) {
		return content[Math.abs( hash( key ) % content.length )];
	}

	/**
	 * The same hash function as Java's String.toString().
	 * <p>
	 * This does not delegate to String.toString() in order to protect against
	 * future changes in the JDK (?) or different JDK implementations,
	 * so that the resulting hash can safely be used for persistence
	 * (e.g. to route data to a file).
	 */
	private static int hash(String key) {
		int hash = 0;
		int length = key.length();
		for ( int index = 0; index < length; index++ ) {
			hash = 31 * hash + key.charAt( index );
		}
		return hash;
	}

}
