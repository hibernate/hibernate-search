/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

/**
 * @author Hardy Ferentschik
 */
public class FacetRange<T> {
	private static final String MIN_INCLUDED = "[";
	private static final String MIN_EXCLUDED = "(";
	private static final String MAX_INCLUDED = "]";
	private static final String MAX_EXCLUDED = ")";

	private static final String IDENTIFIER_INCLUDED = "i";
	private static final String IDENTIFIER_EXCLUDED = "e";
	private static final String IDENTIFIER_SEPARATOR = "_";

	private final T min;
	private final T max;
	private final boolean includeMin;
	private final boolean includeMax;
	private final String rangeString;
	private final String fieldName;
	private final Class<?> rangeType;

	public FacetRange(Class<?> rangeType,
			T min,
			T max,
			boolean includeMin,
			boolean includeMax,
			String fieldName) {
		if ( max == null && min == null ) {
			throw new IllegalArgumentException( "At least one end of the range has to be specified" );
		}

		this.min = min;
		this.max = max;
		this.includeMax = includeMax;
		this.includeMin = includeMin;
		this.fieldName = fieldName;
		this.rangeString = buildRangeString();
		this.rangeType = rangeType;
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

	/**
	 * @return a string suitable to use as an identifier.
	 */
	public String getIdentifier() {
		StringBuilder builder = new StringBuilder();
		if ( includeMin ) {
			builder.append( IDENTIFIER_INCLUDED );
		}
		else {
			builder.append( IDENTIFIER_EXCLUDED );
		}
		if ( min != null ) {
			builder.append( min );
		}
		builder.append( IDENTIFIER_SEPARATOR );
		if ( max != null ) {
			builder.append( max );
		}
		if ( includeMax ) {
			builder.append( IDENTIFIER_INCLUDED );
		}
		else {
			builder.append( IDENTIFIER_EXCLUDED );
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		return "FacetRange"
				+ "{min=" + min
				+ ", max=" + max
				+ ", includeMin=" + includeMin
				+ ", includeMax=" + includeMax
				+ ", fieldName='" + fieldName + '\''
				+ ", rangeType=" + rangeType + '}';
	}
}


