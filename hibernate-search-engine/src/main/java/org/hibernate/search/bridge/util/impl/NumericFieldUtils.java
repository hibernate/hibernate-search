package org.hibernate.search.bridge.util.impl;

import org.apache.lucene.document.NumericField;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.SearchException;

/**
 * Utility class to handle Numeric Fields
 *
 * @author Gustavo Fernandes
 */
public class NumericFieldUtils {

	public static Query createNumericRangeQuery(String fieldName, Object from, Object to,
												boolean includeLower, boolean includeUpper) {

		Class numericClass;

		if ( from != null ) {
			numericClass = from.getClass();
		}
		else if ( to != null ) {
			numericClass = to.getClass();
		}
		else {
			throw new SearchException(
				"Cannot create numeric range query for field " + fieldName + ", since from and to values are " +
						"null");
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
		// TODO: check for type before in the mapping
		throw new SearchException(
				"Cannot create numeric range query for field " + fieldName + ", since values are not numeric " +
						"(int,long, short or double) ");
	}

	/**
	 * Will create a RangeQuery matching exactly the provided value: lower
	 * and upper value match, and bounds are included. This should perform
	 * as efficiently as a TermQuery.
	 * @param fieldName
	 * @param value
	 * @return the created Query
	 */
	public static Query createExactMatchQuery(String fieldName, Object value) {
		return createNumericRangeQuery(fieldName, value, value, true, true);
	}

	public static void setNumericValue(Object value, NumericField numericField) {
		Class numericClass = value.getClass();
		if ( numericClass.isAssignableFrom( Double.class ) ) {
			numericField.setDoubleValue( (Double) value );
		}
		if ( numericClass.isAssignableFrom( Long.class ) ) {
			numericField.setLongValue( (Long) value );
		}
		if ( numericClass.isAssignableFrom( Integer.class ) ) {
			numericField.setIntValue( (Integer) value );
		}
		if ( numericClass.isAssignableFrom( Float.class ) ) {
			numericField.setFloatValue( (Float) value );
		}
	}
}
