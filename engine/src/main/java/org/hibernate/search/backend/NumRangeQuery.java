/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

/**
 * @author Martin Braun
 */
public class NumRangeQuery implements DeletionQuery {

	public static final int QUERY_KEY = 2;

	private final String fieldName;
	private final Type type;
	private final Number min;
	private final Number max;
	private final boolean minInclusive;
	private final boolean maxInclusive;
	private final Integer precisionStep;

	public NumRangeQuery(String fieldName, Integer min, Integer max, boolean minInclusive, boolean maxInclusive, int precisionStep) {
		this( fieldName, Type.INT, min, max, minInclusive, maxInclusive, precisionStep );
	}

	public NumRangeQuery(String fieldName, Integer min, Integer max, boolean minInclusive, boolean maxInclusive) {
		this( fieldName, Type.INT, min, max, minInclusive, maxInclusive, null );
	}

	public NumRangeQuery(String fieldName, Long min, Long max, boolean minInclusive, boolean maxInclusive, int precisionStep) {
		this( fieldName, Type.LONG, min, max, minInclusive, maxInclusive, precisionStep );
	}

	public NumRangeQuery(String fieldName, Long min, Long max, boolean minInclusive, boolean maxInclusive) {
		this( fieldName, Type.LONG, min, max, minInclusive, maxInclusive, null );
	}

	public NumRangeQuery(String fieldName, Float min, Float max, boolean minInclusive, boolean maxInclusive, int precisionStep) {
		this( fieldName, Type.FLOAT, min, max, minInclusive, maxInclusive, precisionStep );
	}

	public NumRangeQuery(String fieldName, Float min, Float max, boolean minInclusive, boolean maxInclusive) {
		this( fieldName, Type.FLOAT, min, max, minInclusive, maxInclusive, null );
	}

	public NumRangeQuery(String fieldName, Double min, Double max, boolean minInclusive, boolean maxInclusive, int precisionStep) {
		this( fieldName, Type.DOUBLE, min, max, minInclusive, maxInclusive, precisionStep );
	}

	public NumRangeQuery(String fieldName, Double min, Double max, boolean minInclusive, boolean maxInclusive) {
		this( fieldName, Type.DOUBLE, min, max, minInclusive, maxInclusive, null );
	}

	public NumRangeQuery(String fieldName, Type type, Number min, Number max, boolean minInclusive, boolean maxInclusive) {
		this( fieldName, type, min, max, minInclusive, maxInclusive, null );
	}

	public NumRangeQuery(String fieldName, Type type, Number min, Number max, boolean minInclusive, boolean maxInclusive, Integer precisionStep) {
		this.fieldName = fieldName;
		this.type = type;
		this.min = min;
		this.max = max;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
		this.precisionStep = precisionStep;
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return the min
	 */
	public Number getMin() {
		return min;
	}

	/**
	 * @return the max
	 */
	public Number getMax() {
		return max;
	}

	/**
	 * @return the minInclusive
	 */
	public boolean isMinInclusive() {
		return minInclusive;
	}

	/**
	 * @return the maxInclusive
	 */
	public boolean isMaxInclusive() {
		return maxInclusive;
	}

	/**
	 * @return the precisionStep
	 */
	public Integer getPrecisionStep() {
		return precisionStep;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( fieldName == null ) ? 0 : fieldName.hashCode() );
		result = prime * result + ( ( max == null ) ? 0 : max.hashCode() );
		result = prime * result + ( maxInclusive ? 1231 : 1237 );
		result = prime * result + ( ( min == null ) ? 0 : min.hashCode() );
		result = prime * result + ( minInclusive ? 1231 : 1237 );
		result = prime * result + ( ( precisionStep == null ) ? 0 : precisionStep.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		NumRangeQuery other = (NumRangeQuery) obj;
		if ( fieldName == null ) {
			if ( other.fieldName != null ) {
				return false;
			}
		}
		else if ( !fieldName.equals( other.fieldName ) ) {
			return false;
		}
		if ( max == null ) {
			if ( other.max != null ) {
				return false;
			}
		}
		else if ( !max.equals( other.max ) ) {
			return false;
		}
		if ( maxInclusive != other.maxInclusive ) {
			return false;
		}
		if ( min == null ) {
			if ( other.min != null ) {
				return false;
			}
		}
		else if ( !min.equals( other.min ) ) {
			return false;
		}
		if ( minInclusive != other.minInclusive ) {
			return false;
		}
		if ( precisionStep == null ) {
			if ( other.precisionStep != null ) {
				return false;
			}
		}
		else if ( !precisionStep.equals( other.precisionStep ) ) {
			return false;
		}
		if ( type != other.type ) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NumRangeQuery [fieldName=" + fieldName + ", type=" + type + ", min=" + min + ", max=" + max + ", minInclusive=" + minInclusive
				+ ", maxInclusive=" + maxInclusive + ", precisionStep=" + precisionStep + "]";
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.search.backend.DeletionQuery#getQueryKey()
	 */
	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	public enum Type {
		INT, LONG, FLOAT, DOUBLE
	}

}
