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

	public static final int QUERY_KEY = 1;

	private final String fieldName;
	private final String value;

	public SingularTermQuery(String key, String value) {
		this.fieldName = key;
		this.value = value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	@Override
	public String toString() {
		return "SingularTermQuery: +" + fieldName + ":" + value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( fieldName == null ) ? 0 : fieldName.hashCode() );
		result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
		return result;
	}

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

}
