/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import java.io.IOException;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.SortField;

public class LuceneNestedNumericFieldComparatorSource extends LuceneNestedFieldComparator {

	private final SortField.Type type;
	private final Object missingValue;

	public LuceneNestedNumericFieldComparatorSource(SortField.Type type, Object missingValue) {
		this.type = type;
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		switch ( type ) {
			case INT:
				return new FieldComparator.IntComparator( numHits, fieldname, (Integer) missingValue ) {
					@Override
					public void copy(int slot, int doc) throws IOException {
						super.copy( slot, getNestedDocument( doc ) );
					}
				};

			case FLOAT:
				return new FieldComparator.FloatComparator( numHits, fieldname, (Float) missingValue ) {
					@Override
					public void copy(int slot, int doc) throws IOException {
						super.copy( slot, getNestedDocument( doc ) );
					}
				};

			case LONG:
				return new FieldComparator.LongComparator( numHits, fieldname, (Long) missingValue ) {
					@Override
					public void copy(int slot, int doc) throws IOException {
						super.copy( slot, getNestedDocument( doc ) );
					}
				};

			case DOUBLE:
				return new FieldComparator.DoubleComparator( numHits, fieldname, (Double) missingValue ) {
					@Override
					public void copy(int slot, int doc) throws IOException {
						super.copy( slot, getNestedDocument( doc ) );
					}
				};

			default:
				throw new IllegalStateException( "Illegal sort type: " + type );
		}
	}
}
