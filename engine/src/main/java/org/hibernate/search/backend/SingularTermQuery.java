/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

/**
 * DeleteByQuery equivalent to {@link org.apache.lucene.search.TermQuery} and supports only Strings as values
 *
 * @author Martin Braun
 */
public class SingularTermQuery implements DeletionQuery {

	public static final int QUERY_KEY = 0;

	private final String fieldName;
	private final Object value;
	private final Type type;

	public SingularTermQuery(String fieldName, String value) {
		this( fieldName, value, Type.STRING );
	}

	public SingularTermQuery(String fieldName, int value) {
		this( fieldName, value, Type.INT );
	}

	public SingularTermQuery(String fieldName, long value) {
		this( fieldName, value, Type.LONG );
	}

	public SingularTermQuery(String fieldName, float value) {
		this( fieldName, value, Type.FLOAT );
	}

	public SingularTermQuery(String fieldName, double value) {
		this( fieldName, value, Type.DOUBLE );
	}

	public SingularTermQuery(String fieldName, Object value, Type type) {
		this.fieldName = fieldName;
		this.value = value;
		this.type = type;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Object getValue() {
		return value;
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	@Override
	public String toString() {
		return "SingularTermQuery: +" + fieldName + ":" + value;
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
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
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
		SingularTermQuery other = (SingularTermQuery) obj;
		if ( fieldName == null ) {
			if ( other.fieldName != null ) {
				return false;
			}
		}
		else if ( !fieldName.equals( other.fieldName ) ) {
			return false;
		}
		if ( type != other.type ) {
			return false;
		}
		if ( value == null ) {
			if ( other.value != null ) {
				return false;
			}
		}
		else if ( !value.equals( other.value ) ) {
			return false;
		}
		return true;
	}

	public enum Type {
		STRING, INT, LONG, FLOAT, DOUBLE
	}

}
