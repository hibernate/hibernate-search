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
import org.hibernate.search.bridge.impl.JavaTimeBridgeProvider;
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
		if ( JavaTimeBridgeProvider.isActive() ) {
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
	 * This should match the default {@code FieldBridge} used for each type.
	 * @param value on Object
	 * @return true if the value argument is of any type which is by default indexed as a NumericField
	 */
	public static boolean requiresNumericRangeQuery(Object value) {
		if ( value == null ) {
			return false;
		}
		final Class<?> numericClass = value.getClass();
		return Double.class.isAssignableFrom( numericClass ) ||
				Long.class.isAssignableFrom( numericClass ) ||
				Integer.class.isAssignableFrom( numericClass ) ||
				Float.class.isAssignableFrom( numericClass ) ||
				Calendar.class.isAssignableFrom( numericClass ) ||
				( JavaTimeBridgeProvider.isActive() && (
					java.time.Instant.class.isAssignableFrom( numericClass ) ||
					java.time.Year.class.isAssignableFrom( numericClass ) ||
					java.time.Duration.class.isAssignableFrom( numericClass )
				));
	}
}
