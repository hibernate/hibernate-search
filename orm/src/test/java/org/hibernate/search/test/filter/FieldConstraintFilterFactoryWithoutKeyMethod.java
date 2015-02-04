/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.filter.impl.CachingWrapperFilter;

/**
 * Creates a filter for a given field, providing access to all invocations of {@link #buildFilter()} for testing
 * purposes.
 *
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class FieldConstraintFilterFactoryWithoutKeyMethod {

	private static List<BuildFilterInvocation> builtFilters = new ArrayList<>();

	private String field;
	private String value;

	@Factory
	public Filter buildFilter() {
		builtFilters.add( new BuildFilterInvocation( field, value ) );

		Query q = new TermQuery( new Term( field, value ) );
		Filter filter = new QueryWrapperFilter( q );
		filter = new CachingWrapperFilter( filter );

		return filter;
	}

	public void setField(String field) {
		this.field = field;
	}

	public void setValue(String value) {
		this.value = value;
	}

	static List<BuildFilterInvocation> getBuiltFilters() {
		return builtFilters;
	}

	/**
	 * Represents one invocation of {@link FieldConstraintFilterFactoryWithoutKeyMethod#buildFilter()}.
	 */
	static class BuildFilterInvocation {
		private final String field;
		private final String value;

		public BuildFilterInvocation(String field, String value) {
			this.field = field;
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public String getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( field == null ) ? 0 : field.hashCode() );
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
			BuildFilterInvocation other = (BuildFilterInvocation) obj;
			if ( field == null ) {
				if ( other.field != null ) {
					return false;
				}
			}
			else if ( !field.equals( other.field ) ) {
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

		@Override
		public String toString() {
			return "BuildFilterInvocation [field=" + field + ", value=" + value + "]";
		}
	}
}
