/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.util.impl;

import java.util.Calendar;
import java.util.Date;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.ContainerBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.EncodingBridge;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Utility class to handle numeric fields.
 *
 * @author Gustavo Fernandes
 * @author Hardy Ferentschik
 */
public final class NumericFieldUtils {

	private static final Log log = LoggerFactory.make();

	private NumericFieldUtils() {
		//not allowed
	}

	public static Query createNumericRangeQuery(String fieldName, Object from, Object to,
												boolean includeLower, boolean includeUpper) {

		Class<?> numericClass;

		if ( from != null ) {
			numericClass = from.getClass();
		}
		else if ( to != null ) {
			numericClass = to.getClass();
		}
		else {
			throw log.rangeQueryWithNullToAndFromValue( fieldName );
		}

		if ( Double.class.isAssignableFrom( numericClass ) ) {
			return NumericRangeQuery.newDoubleRange( fieldName, (Double) from, (Double) to, includeLower, includeUpper );
		}
		if ( Byte.class.isAssignableFrom( numericClass ) ) {
			return NumericRangeQuery.newIntRange( fieldName, ( (Byte) from ).intValue(), ( (Byte) to ).intValue(), includeLower, includeUpper );
		}
		if ( Short.class.isAssignableFrom( numericClass ) ) {
			return NumericRangeQuery.newIntRange( fieldName, ( (Short) from ).intValue(), ( (Short) to ).intValue(), includeLower, includeUpper );
		}
		if ( Long.class.isAssignableFrom( numericClass ) ) {
			return NumericRangeQuery.newLongRange( fieldName, (Long) from, (Long) to, includeLower, includeUpper );
		}
		if ( Integer.class.isAssignableFrom( numericClass ) ) {
			return NumericRangeQuery.newIntRange( fieldName, (Integer) from, (Integer) to, includeLower, includeUpper );
		}
		if ( Float.class.isAssignableFrom( numericClass ) ) {
			return NumericRangeQuery.newFloatRange( fieldName, (Float) from, (Float) to, includeLower, includeUpper );
		}
		if ( Date.class.isAssignableFrom( numericClass ) ) {
			Long fromValue = from != null ? ((Date) from).getTime() : null;
			Long toValue = to != null ? ((Date) to).getTime() : null;
			return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}
		if ( Calendar.class.isAssignableFrom( numericClass ) ) {
			Long fromValue = from != null ? ((Calendar) from).getTime().getTime() : null;
			Long toValue = to != null ? ((Calendar) to).getTime().getTime() : null;
			return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}
		if ( java.time.Duration.class.isAssignableFrom( numericClass ) ) {
			Long fromValue = from != null ? ( (java.time.Duration) from ).toNanos() : null;
			Long toValue = to != null ? ( (java.time.Duration) to ).toNanos() : null;
			return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}
		if ( java.time.Year.class.isAssignableFrom( numericClass ) ) {
			Integer fromValue = from != null ? ( (java.time.Year) from ).getValue() : null;
			Integer toValue = to != null ? ( (java.time.Year) to ).getValue() : null;
			return NumericRangeQuery.newIntRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}
		if ( java.time.Instant.class.isAssignableFrom( numericClass ) ) {
			Long fromValue = from != null ? ( (java.time.Instant) from ).toEpochMilli() : null;
			Long toValue = to != null ? ( (java.time.Instant) to ).toEpochMilli() : null;
			return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}

		throw log.numericRangeQueryWithNonNumericToAndFromValues( fieldName );
	}

	/**
	 * Will create a {@code RangeQuery} matching exactly the provided value: lower
	 * and upper value match, and bounds are included. This should perform
	 * as efficiently as a TermQuery.
	 *
	 * @param fieldName the field name the query targets
	 * @param value the value to match
	 * @return the created {@code Query}
	 */
	public static Query createExactMatchQuery(String fieldName, Object value) {
		return createNumericRangeQuery( fieldName, value, value, true, true );
	}

	/**
	 * When the type of {@code RangeQuery} needs to be guessed among keyword based ranges or numeric based
	 * range queries, the parameter type defines the strategy.
	 *
	 * Note that this is currently only used by the Infinispan backend as a fallback and it should be used with a lot
	 * of caution as it does not take into account backend specific behaviors.
	 * For instance, when indexing on Elasticsearch, Dates require a keyword range query.
	 *
	 * This should match the default {@code FieldBridge} used for each type.
	 * @param value on Object
	 * @return true if the value argument is of any type which is by default indexed as a NumericField
	 */
	public static boolean requiresNumericRangeQuery(Object value) {
		if ( value == null ) {
			return false;
		}
		return value instanceof Double ||
				value instanceof Byte ||
				value instanceof Short ||
				value instanceof Long ||
				value instanceof Integer ||
				value instanceof Float ||
				value instanceof Date ||
				value instanceof Calendar ||
				value instanceof java.time.Instant ||
				value instanceof java.time.Year ||
				value instanceof java.time.Duration;
	}

	/**
	 * Indicates whether the considered {@code FieldBridge} is a numeric one.
	 *
	 * @param fieldBridge the considered {@code FieldBridge}
	 * @return true if the considered {@code FieldBridge} is a numeric {@code FieldBridge}
	 */
	public static boolean isNumericFieldBridge(FieldBridge fieldBridge) {
		EncodingBridge encodingBridge = BridgeAdaptorUtils.unwrapAdaptorOnly( fieldBridge, EncodingBridge.class );
		return !NumericEncodingType.UNKNOWN.equals( getNumericEncoding( encodingBridge ) );
	}

	/**
	 * Indicates whether the considered {@code FieldBridge}, or its {@link ContainerBridge#getElementBridge() element bridge},
	 * is a numeric one.
	 *
	 * @param fieldBridge the considered {@code FieldBridge}
	 * @return true if the considered {@code FieldBridge} is a numeric {@code FieldBridge}
	 */
	public static boolean isNumericContainerOrNumericFieldBridge(FieldBridge fieldBridge) {
		EncodingBridge encodingBridge = BridgeAdaptorUtils.unwrapAdaptorAndContainer( fieldBridge, EncodingBridge.class );
		return !NumericEncodingType.UNKNOWN.equals( getNumericEncoding( encodingBridge ) );
	}

	private static NumericEncodingType getNumericEncoding(EncodingBridge encodingBridge) {
		if ( encodingBridge != null ) {
			return encodingBridge.getEncodingType();
		}
		else {
			return NumericEncodingType.UNKNOWN;
		}
	}
}
