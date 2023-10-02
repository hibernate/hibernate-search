/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

/**
 * @author Hardy Ferentschik
 */
public class FacetRange<T> {
	private static final String MIN_INCLUDED = "[";
	private static final String MIN_EXCLUDED = "(";
	private static final String MAX_INCLUDED = "]";
	private static final String MAX_EXCLUDED = ")";

	private final Range<T> range;
	private final String rangeString;
	private final String fieldName;
	private final Class<?> rangeType;

	public FacetRange(Class<?> rangeType, Range<T> range, String fieldName) {
		this.range = range;
		this.fieldName = fieldName;
		this.rangeString = buildRangeString();
		this.rangeType = rangeType;
	}

	public Range<T> range() {
		return range;
	}

	public T getMin() {
		return range.lowerBoundValue().orElse( null );
	}

	public T getMax() {
		return range.upperBoundValue().orElse( null );
	}

	public boolean isMinIncluded() {
		return RangeBoundInclusion.INCLUDED.equals( range.lowerBoundInclusion() );
	}

	public boolean isMaxIncluded() {
		return RangeBoundInclusion.INCLUDED.equals( range.upperBoundInclusion() );
	}

	public String getRangeString() {
		return rangeString;
	}

	private String buildRangeString() {
		StringBuilder builder = new StringBuilder();
		if ( isMinIncluded() ) {
			builder.append( MIN_INCLUDED );
		}
		else {
			builder.append( MIN_EXCLUDED );
		}
		if ( getMin() != null ) {
			builder.append( getMin() );
		}
		builder.append( ", " );
		if ( getMax() != null ) {
			builder.append( getMax() );
		}
		if ( isMaxIncluded() ) {
			builder.append( MAX_INCLUDED );
		}
		else {
			builder.append( MAX_EXCLUDED );
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		return "FacetRange"
				+ "{min=" + getMin()
				+ ", max=" + getMax()
				+ ", includeMin=" + isMinIncluded()
				+ ", includeMax=" + isMaxIncluded()
				+ ", fieldName='" + fieldName + '\''
				+ ", rangeType=" + rangeType + '}';
	}
}

