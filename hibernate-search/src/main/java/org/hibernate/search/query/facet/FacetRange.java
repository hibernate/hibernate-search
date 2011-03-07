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

package org.hibernate.search.query.facet;

/**
 * @author Hardy Ferentschik
 */
public class FacetRange<N extends Number> {
	private static final String MIN_INCLUDED = "[";
	private static final String MIN_EXCLUDED = "(";
	private static final String MAX_INCLUDED = "]";
	private static final String MAX_EXCLUDED = ")";

	private final N min;
	private final N max;
	private final boolean includeMin;
	private final boolean includeMax;
	private final String rangeString;

	public FacetRange(N min, N max) {
		this( min, max, true, true );
	}

	public FacetRange(N min, N max, boolean includeMin, boolean includeMax) {
		this.min = min;
		this.max = max;
		this.includeMax = includeMax;
		this.includeMin = includeMin;

		StringBuilder builder = new StringBuilder();
		if ( includeMin ) {
			builder.append( MIN_INCLUDED );
		}
		else {
			builder.append( MIN_EXCLUDED );
		}
		builder.append( min );
		builder.append( ", " );
		builder.append( max );

		if ( includeMax ) {
			builder.append( MAX_INCLUDED );
		}
		else {
			builder.append( MAX_EXCLUDED );
		}
		this.rangeString = builder.toString();
	}

	public N getMin() {
		return min;
	}

	public N getMax() {
		return max;
	}

	public boolean isIncludeMin() {
		return includeMin;
	}

	public boolean isIncludeMax() {
		return includeMax;
	}

	public boolean isInRange(N value) {
		int minCheck = compare( min, value );
		if ( isIncludeMin() && minCheck > 0 ) {
			return false;
		}
		else if ( !isIncludeMin() && minCheck >= 0 ) {
			return false;
		}

		int maxCheck = compare( value, max );
		if ( isIncludeMax() && maxCheck > 0 ) {
			return false;
		}
		else if ( !isIncludeMax() && maxCheck >= 0 ) {
			return false;
		}
		return true;
	}

	public String getRangeString() {
		return rangeString;
	}

	// todo - does this implementation of Number comparison hold?
	private int compare(N number1, N number2) {
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
		return "FacetRange{" +
				"min=" + min +
				", max=" + max +
				", includeMin=" + includeMin +
				", includeMax=" + includeMax +
				'}';
	}
}


