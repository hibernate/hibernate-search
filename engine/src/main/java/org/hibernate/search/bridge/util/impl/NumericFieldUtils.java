/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.util.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.builtin.time.impl.DurationNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.InstantNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalDateNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalDateTimeNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.LocalTimeNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.MonthDayNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.YearMonthNumericBridge;
import org.hibernate.search.bridge.builtin.time.impl.YearNumericBridge;
import org.hibernate.search.engine.Version;
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

	private static final Class<?>[] NUMERIC_JAVA_TIME_TYPES = { Year.class, Instant.class, LocalDate.class, LocalDateTime.class, YearMonth.class,
			MonthDay.class, Duration.class, LocalDateTime.class };

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

		if ( numericClass.isAssignableFrom( Double.class ) ) {
			return NumericRangeQuery.newDoubleRange( fieldName, (Double) from, (Double) to, includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Byte.class ) ) {
			return NumericRangeQuery.newIntRange( fieldName, ( (Byte) from ).intValue(), ( (Byte) to ).intValue(), includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Short.class ) ) {
			return NumericRangeQuery.newIntRange( fieldName, ( (Short) from ).intValue(), ( (Short) to ).intValue(), includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Long.class ) ) {
			return NumericRangeQuery.newLongRange( fieldName, (Long) from, (Long) to, includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Integer.class ) ) {
			return NumericRangeQuery.newIntRange( fieldName, (Integer) from, (Integer) to, includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Float.class ) ) {
			return NumericRangeQuery.newFloatRange( fieldName, (Float) from, (Float) to, includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Date.class ) ) {
			Long fromValue = from != null ? ((Date) from).getTime() : null;
			Long toValue = to != null ? ((Date) to).getTime() : null;
			return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}
		if ( numericClass.isAssignableFrom( Calendar.class ) ) {
			Long fromValue = from != null ? ((Calendar) from).getTime().getTime() : null;
			Long toValue = to != null ? ((Calendar) to).getTime().getTime() : null;
			return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
		}
		if ( jdk8Compatible() ) {
			if ( numericClass.isAssignableFrom( LocalDate.class ) ) {
				Long fromValue = from != null ? LocalDateNumericBridge.INSTANCE.encode( (LocalDate) from ) : null;
				Long toValue = to != null ? LocalDateNumericBridge.INSTANCE.encode( (LocalDate) to ) : null;
				return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( LocalTime.class ) ) {
				Long fromValue = from != null ? LocalTimeNumericBridge.INSTANCE.encode( (LocalTime) from ) : null;
				Long toValue = to != null ? LocalTimeNumericBridge.INSTANCE.encode( (LocalTime) to ) : null;
				return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( LocalDateTime.class ) ) {
				Long fromValue = from != null ? LocalDateTimeNumericBridge.INSTANCE.encode( (LocalDateTime) from ) : null;
				Long toValue = to != null ? LocalDateTimeNumericBridge.INSTANCE.encode( (LocalDateTime) to ) : null;
				return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( Year.class ) ) {
				int fromValue = from != null ? YearNumericBridge.INSTANCE.encode( ( (Year) from ) ) : null;
				int toValue = to != null ? YearNumericBridge.INSTANCE.encode( ( (Year) to ) ) : null;
				return NumericRangeQuery.newIntRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( YearMonth.class ) ) {
				long fromValue = from != null ? YearMonthNumericBridge.INSTANCE.encode( ( (YearMonth) from ) ) : null;
				long toValue = to != null ? YearMonthNumericBridge.INSTANCE.encode( ( (YearMonth) to ) ) : null;
				return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( MonthDay.class ) ) {
				int fromValue = from != null ? MonthDayNumericBridge.INSTANCE.encode( ( (MonthDay) from ) ) : null;
				int toValue = to != null ? MonthDayNumericBridge.INSTANCE.encode( ( (MonthDay) to ) ) : null;
				return NumericRangeQuery.newIntRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( Duration.class ) ) {
				long fromValue = from != null ? DurationNumericBridge.INSTANCE.encode( ( (Duration) from ) ) : null;
				long toValue = to != null ? DurationNumericBridge.INSTANCE.encode( ( (Duration) to ) ) : null;
				return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
			if ( numericClass.isAssignableFrom( Instant.class ) ) {
				long fromValue = from != null ? InstantNumericBridge.INSTANCE.encode( ( (Instant) from ) ) : null;
				long toValue = to != null ? InstantNumericBridge.INSTANCE.encode( ( (Instant) to ) ) : null;
				return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
			}
		}

		throw log.numericRangeQueryWithNonNumericToAndFromValues( fieldName );
	}

	private static boolean jdk8Compatible() {
		return Version.getJavaRelease() >= 8;
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
		return numericClass.isAssignableFrom( Double.class )
				|| numericClass.isAssignableFrom( Long.class )
				|| numericClass.isAssignableFrom( Integer.class )
				|| numericClass.isAssignableFrom( Float.class )
				|| numericClass.isAssignableFrom( Calendar.class )
				|| (jdk8Compatible() && numericTemporalClass( numericClass ) );
	}

	private static boolean numericTemporalClass(Class<?> numericClass) {
		for ( Class<?> type : NUMERIC_JAVA_TIME_TYPES ) {
			if ( numericClass.isAssignableFrom( type ) ) {
				return true;
			}
		}
		return false;
	}
}
