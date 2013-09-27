/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
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
public final class NumericFieldUtils {

	private NumericFieldUtils() {
		//not allowed
	}

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
		return createNumericRangeQuery( fieldName, value, value, true, true );
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
