/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.filter.StandardFilterKey;
import org.hibernate.search.filter.impl.CachingWrapperFilter;

public class FieldConstraintFilter {
	private String field;
	private String value;

	@org.hibernate.search.annotations.Factory
	public Filter buildFilter() {
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

	@org.hibernate.search.annotations.Key
	public org.hibernate.search.filter.FilterKey getKey() {
		StandardFilterKey key = new StandardFilterKey();
		key.addParameter( field );
		key.addParameter( value );
		return key;
	}
}
