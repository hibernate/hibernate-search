/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Bits;
import org.hibernate.search.filter.impl.CachingWrapperFilter;

/**
 * A filter based on a given value for a given field.
 *
 * @author Gunnar Morling
 */
public class FieldConstraintFilterWithoutKeyMethod extends Filter {

	private static List<FieldConstraintFilterWithoutKeyMethod> instances = new ArrayList<>();

	private String field;
	private String value;

	// used by the engine
	public FieldConstraintFilterWithoutKeyMethod() {
		instances.add( this );
	}

	// used for assertions only
	FieldConstraintFilterWithoutKeyMethod(String field, String value) {
		this.field = field;
		this.value = value;
	}

	public void setField(String field) {
		this.field = field;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
		Query q = new TermQuery( new Term( field, value ) );
		Filter filter = new QueryWrapperFilter( q );
		filter = new CachingWrapperFilter( filter );

		return filter.getDocIdSet( context, acceptDocs );
	}


	public static List<FieldConstraintFilterWithoutKeyMethod> getInstances() {
		return instances;
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
		FieldConstraintFilterWithoutKeyMethod other = (FieldConstraintFilterWithoutKeyMethod) obj;
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
		return "FieldConstraintFilterWithoutKeyMethod [field=" + field + ", value=" + value + "]";
	}
}
