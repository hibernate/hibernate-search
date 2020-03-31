/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene;

import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.join.ScoreMode;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class ValueSortField extends SortField {

	private Query filter;
	private ScoreMode mode;
	private Object nullAsValue;

	public ValueSortField(String field, Type type) {
		super( field, type );
	}

	public ValueSortField(String field, Type type, boolean reverse) {
		super( field, type, reverse );
	}

	public ValueSortField(String field, FieldComparatorSource comparator) {
		super( field, comparator );
	}

	public ValueSortField(String field, FieldComparatorSource comparator, boolean reverse) {
		super( field, comparator, reverse );
	}

	public ValueSortField mode(ScoreMode mode) {
		this.mode = mode;
		return this;
	}

	public ValueSortField filter(Query filter) {
		this.filter = filter;
		return this;
	}

	public ValueSortField nullAsValue(Object nullAsValue) {
		this.nullAsValue = nullAsValue;
		return this;
	}

	public Query getFilter() {
		return filter;
	}

	public void setFilter(Query filter) {
		this.filter = filter;
	}

	public ScoreMode getMode() {
		return mode;
	}

	public void setMode(ScoreMode mode) {
		this.mode = mode;
	}

	public Object getNullAsValue() {
		return nullAsValue;
	}

	public void setNullAsValue(Object nullAsValue) {
		this.nullAsValue = nullAsValue;
	}

}
