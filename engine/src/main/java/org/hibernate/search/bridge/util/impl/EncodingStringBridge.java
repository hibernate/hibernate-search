/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.util.impl;

import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.spi.EncodingBridge;
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;


/**
 * A base class for {@link EncodingBridge}s that encode values as Strings using a specific format.
 *
 * @author Yoann Rodiere
 */
public abstract class EncodingStringBridge<T> implements EncodingBridge, StringBridge {

	@Override
	public NumericEncodingType getEncodingType() {
		// Nulls are encoded as strings
		return NumericEncodingType.UNKNOWN;
	}

	/**
	 * Parse the 'indexNullAs' string to an indexable value.
	 * <p>The required format does not have to be the same as the encoded format.
	 *
	 * @param indexNullAs The string to parse.
	 * @return The encoded value for the given string.
	 * @throws IllegalArgumentException If the given string does not match the required format.
	 */
	protected abstract T parseIndexNullAs(String indexNullAs) throws IllegalArgumentException;

	@Override
	public NullMarker createNullMarker(String indexNullAs) throws IllegalArgumentException {
		// Parse the given "indexNullAs" string
		T typedValue = parseIndexNullAs( indexNullAs );
		// Convert the resulting date to the format used when indexing.
		String encodedValue = objectToString( typedValue );
		return new ToStringNullMarker( encodedValue );
	}

}
