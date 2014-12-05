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

		if ( numericClass.isAssignableFrom( Double.class ) ) {
			return NumericRangeQuery.newDoubleRange( fieldName, (Double) from, (Double) to, includeLower, includeUpper );
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

}
