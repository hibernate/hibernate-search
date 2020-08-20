/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

/**
 * {@link StringBridge} allowing a translation from the string representation back to the {@code Object}.
 * {@code objectToString( stringToObject( string ) )} and {@code stringToObject( objectToString( object ) )}
 * should be "idempotent". More precisely:
 * <ul>
 * <li>{@code objectToString( stringToObject( string ) ).equals(string)}, for non {@code null} string.</li>
 * <li>{@code stringToObject( objectToString( object ) ).equals(object)}, for non {@code null} object.</li>
 * </ul>
 *
 * As for all Bridges implementations must be threadsafe.
 *
 * @author Emmanuel Bernard
 */
public interface TwoWayStringBridge extends StringBridge {

	/**
	 * Convert the index string representation to an object.
	 *
	 * @param stringValue The index value.
	 * @return Takes the string representation from the Lucene index and transforms it back into the original
	 * <code>Object</code>.
	 */
	Object stringToObject(String stringValue);
}
