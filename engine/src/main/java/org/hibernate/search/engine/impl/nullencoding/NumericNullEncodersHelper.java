/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl.nullencoding;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Sanne Grinovero
 */
public class NumericNullEncodersHelper {

	private static final Log log = LoggerFactory.make();

	private NumericNullEncodersHelper() {
		// not to be instantiated
	}

	public static NullMarkerCodec createNumericNullMarkerCodec(NumericEncodingType numericEncodingType, String indexNullAs, String fieldName) {
		switch ( numericEncodingType ) {
			case DOUBLE:
				return new NumericDoubleNullCodec( toDouble( indexNullAs, fieldName ) );
			case FLOAT:
				return new NumericFloatNullCodec( toFloat( indexNullAs, fieldName ) );
			case INTEGER:
				return new NumericIntegerNullCodec( toInteger( indexNullAs, fieldName ) );
			case LONG:
				return new NumericLongNullCodec( toLong( indexNullAs, fieldName ) );
			default:
				throw new AssertionFailure( "this should never be invoked for non-Numeric fields" );
		}
	}

	private static Long toLong(String proposedTokenValue, String fieldName) {
		try {
			return Long.parseLong( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw log.nullMarkerNeedsToRepresentALongNumber( proposedTokenValue, fieldName );
		}
	}

	private static Integer toInteger(String proposedTokenValue, String fieldName) {
		try {
			return Integer.parseInt( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw log.nullMarkerNeedsToRepresentAnIntegerNumber( proposedTokenValue, fieldName );
		}
	}

	private static Float toFloat(String proposedTokenValue, String fieldName) {
		try {
			return Float.parseFloat( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw log.nullMarkerNeedsToRepresentAFloatNumber( proposedTokenValue, fieldName );
		}
	}

	private static Double toDouble(String proposedTokenValue, String fieldName) {
		try {
			return Double.parseDouble( proposedTokenValue );
		}
		catch (NumberFormatException nfe) {
			throw log.nullMarkerNeedsToRepresentADoubleNumber( proposedTokenValue, fieldName );
		}
	}

}
