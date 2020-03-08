/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class MiltiValueSortField extends SortField {

	private Query filter;
	private NumericMultiValueMode numericMode;

	public MiltiValueSortField(String field, Type type) {
		super( field, type );
	}

	public MiltiValueSortField(String field, Type type, boolean reverse) {
		super( field, type, reverse );
	}

	public MiltiValueSortField(String field, FieldComparatorSource comparator) {
		super( field, comparator );
	}

	public MiltiValueSortField(String field, FieldComparatorSource comparator, boolean reverse) {
		super( field, comparator, reverse );
	}

	public MiltiValueSortField mode(NumericMultiValueMode mode) {
		this.numericMode = mode;
		return this;
	}

	public MiltiValueSortField filter(Query filter) {
		this.filter = filter;
		return this;
	}

	// Used for system sort
	private FieldComparatorSource systemComparatorSource;

	public void setSystemComparatorSource(FieldComparatorSource systemComparatorSource) {
		this.systemComparatorSource = systemComparatorSource;
	}

	@Override
	public FieldComparator<?> getComparator(final int numHits, final int sortPos) {

		switch ( getType() ) {
			case CUSTOM:
				FieldComparatorSource comparatorSource = getComparatorSource();
				assert comparatorSource != null;
				return comparatorSource.newComparator( getField(), numHits, sortPos, getReverse() );
			default:
				assert systemComparatorSource != null;
				return systemComparatorSource.newComparator( getField(), numHits, sortPos, getReverse() );
		}
	}

}
