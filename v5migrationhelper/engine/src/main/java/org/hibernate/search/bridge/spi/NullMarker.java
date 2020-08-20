/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

/**
 * Representation of a missing value.
 *
 * <p>When encoding a marker the index field type shall match the other fields',
 * as field types often need to be consistent across the whole index.
 *
 * @author Sanne Grinovero
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing users.
 */
public interface NullMarker {

	/**
	 * This is mostly a requirement for integration with other old-style
	 * contracts which expect a strongly String based strategy.
	 *
	 * @return a string representation of the null-marker, or null if no marker is used.
	 */
	String nullRepresentedAsString();

	/**
	 * This returns the actual value to be indexed in place of null.
	 *
	 * <p>The returned value may be:
	 * <ul>
	 * <li>A String, for text fields.
	 * <li>A Number, for numeric fields.
	 * </ul>
	 *
	 * @return a representation of the null-marker ready to be indexed.
	 */
	Object nullEncoded();

}
