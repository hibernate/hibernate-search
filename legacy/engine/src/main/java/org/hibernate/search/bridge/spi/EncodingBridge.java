/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Interface implemented by bridges that encode information in a specific format.
 *
 * <p>Those bridges should:
 * <ul>
 * <li>declare the numeric encoding, if any, in order to expose it in the metadata
 * <li>declare the null marker, in order to enable {@link Field#indexNullAs()}.
 * </ul>
 *
 * <p>Bridges that do not implement this interface will be attributed a {@code null}
 * numeric encoding and fields with those bridges will use a simple null marker holding
 * a string.
 *
 * @author Yoann Rodiere
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing users.
 */
public interface EncodingBridge {

	/**
	 * Define the numeric encoding to use for the brdige.
	 *
	 * @return the encoding to use for this bridge, or {@link NumericEncodingType#UNKNOWN} if
	 * this bridge does not use numeric encoding.
	 */
	NumericEncodingType getEncodingType();

	/**
	 * @param indexNullAs The value of {@link Field#indexNullAs()}.
	 * @return The marker to use when a value is missing.
	 * @throws IllegalArgumentException If {@code indexNullAs} cannot be encoded in the required format.
	 */
	NullMarker createNullMarker(String indexNullAs) throws IllegalArgumentException;

}
