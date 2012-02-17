/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.query.dsl.impl;

import java.util.Date;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;

/**
 * @author Hardy Ferentschik
 */
public class FacetRange<T> {
	private static final String MIN_INCLUDED = "[";
	private static final String MIN_EXCLUDED = "(";
	private static final String MAX_INCLUDED = "]";
	private static final String MAX_EXCLUDED = ")";

	private final T min;
	private final T max;
	private final boolean includeMin;
	private final boolean includeMax;
	private final String rangeString;
	private final String fieldName;
	private final Class<?> rangeType;

	private String stringMin;
	private String stringMax;

	public FacetRange(Class<?> rangeType,
					  T min,
					  T max,
					  boolean includeMin,
					  boolean includeMax,
					  String fieldName,
					  DocumentBuilderIndexedEntity<?> documentBuilder) {
		if ( max == null && min == null ) {
			throw new IllegalArgumentException( "At least one end of the range has to be specified" );
		}

		if ( documentBuilder == null ) {
			throw new AssertionFailure(
					"null is not a valid document builder"
			);
		}

		this.min = min;
		this.max = max;
		this.includeMax = includeMax;
		this.includeMin = includeMin;
		this.fieldName = fieldName;
		this.rangeString = buildRangeString();
		this.rangeType = rangeType;

		if ( Date.class.equals( rangeType ) ) {
			final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
			stringMin = documentBuilder.objectToString( fieldName, min, conversionContext );
			stringMax = documentBuilder.objectToString( fieldName, max, conversionContext );
		}
	}

	public T getMin() {
		return min;
	}

	public T getMax() {
		return max;
	}

	public boolean isMinIncluded() {
		return includeMin;
	}

	public boolean isMaxIncluded() {
		return includeMax;
	}

	public boolean isInRange(T value) {
		if ( Number.class.isAssignableFrom( rangeType ) ) {
			return isInRangeNumber( (Number) value, (Number) min, (Number) max );
		}
		else if ( String.class.equals( rangeType ) ) {
			return isInRangeString( (String) value, (String) min, (String) max );
		}
		else if ( Date.class.equals( rangeType ) ) {
			return isInRangeString(
					(String) value,
					stringMin,
					stringMax
			);
		}
		else {
			throw new AssertionFailure( "Unexpected value type: " + value.getClass().getName() );
		}
	}

	public String getRangeString() {
		return rangeString;
	}

	private String buildRangeString() {
		StringBuilder builder = new StringBuilder();
		if ( includeMin ) {
			builder.append( MIN_INCLUDED );
		}
		else {
			builder.append( MIN_EXCLUDED );
		}
		if ( min != null ) {
			builder.append( min );
		}
		builder.append( ", " );
		if ( max != null ) {
			builder.append( max );
		}
		if ( includeMax ) {
			builder.append( MAX_INCLUDED );
		}
		else {
			builder.append( MAX_EXCLUDED );
		}
		return builder.toString();
	}

	private boolean isInRangeString(String value, String min, String max) {
		// below range
		if ( min == null ) {
			if ( isMaxIncluded() ) {
				return value.compareTo( max ) <= 0;
			}
			else {
				return value.compareTo( max ) < 0;
			}
		}

		// above range
		if ( max == null ) {
			if ( isMinIncluded() ) {
				return value.compareTo( min ) >= 0;
			}
			else {
				return value.compareTo( min ) > 0;
			}
		}

		// from .. to range
		int minCheck = min.compareTo( value );
		if ( isMinIncluded() && minCheck > 0 ) {
			return false;
		}
		else if ( !isMinIncluded() && minCheck >= 0 ) {
			return false;
		}

		int maxCheck = value.compareTo( max );
		if ( isMaxIncluded() && maxCheck > 0 ) {
			return false;
		}
		else if ( !isMaxIncluded() && maxCheck >= 0 ) {
			return false;
		}
		return true;
	}

	private boolean isInRangeNumber(Number value, Number min, Number max) {
		// below range
		if ( min == null ) {
			if ( isMaxIncluded() ) {
				return compare( value, max ) <= 0;
			}
			else {
				return compare( value, max ) < 0;
			}
		}

		// above range
		if ( max == null ) {
			if ( isMinIncluded() ) {
				return compare( value, min ) >= 0;
			}
			else {
				return compare( value, min ) > 0;
			}
		}

		// from .. to range
		int minCheck = compare( min, value );
		if ( isMinIncluded() && minCheck > 0 ) {
			return false;
		}
		else if ( !isMinIncluded() && minCheck >= 0 ) {
			return false;
		}

		int maxCheck = compare( value, max );
		if ( isMaxIncluded() && maxCheck > 0 ) {
			return false;
		}
		else if ( !isMaxIncluded() && maxCheck >= 0 ) {
			return false;
		}
		return true;
	}

	// todo - does this implementation of Number comparison hold?
	private int compare(Number number1, Number number2) {
		if ( !number2.getClass().equals( number1.getClass() ) ) {
			throw new IllegalStateException();
		}

		if ( number1 instanceof Comparable ) {
			return ( (Comparable) number1 ).compareTo( number2 );
		}

		if ( number1.doubleValue() < number2.doubleValue() ) {
			return -1;
		}
		if ( number1.doubleValue() > number2.doubleValue() ) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "FacetRange" );
		sb.append( "{min=" ).append( min );
		sb.append( ", max=" ).append( max );
		sb.append( ", includeMin=" ).append( includeMin );
		sb.append( ", includeMax=" ).append( includeMax );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", rangeType=" ).append( rangeType );
		sb.append( '}' );
		return sb.toString();
	}
}


