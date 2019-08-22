/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.types.sort.impl.SortMissingValue;
import org.hibernate.search.backend.lucene.types.sort.missing.impl.LuceneReplaceMissingSortedDocValues;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.util.BytesRef;

public class LuceneNestedTextFieldComparatorSource extends LuceneNestedFieldComparator {

	private final Object missingValue;

	public LuceneNestedTextFieldComparatorSource(Object missingValue) {
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		if ( missingValue == null ) {
			return new FieldComparator.TermOrdValComparator( numHits, fieldname, reversed ) {
				@Override
				public void copy(int slot, int doc) throws IOException {
					super.copy( slot, getNestedDocument( doc ) );
				}
			};
		}

		if ( SortMissingValue.MISSING_FIRST.equals( missingValue ) ) {
			return new FieldComparator.TermOrdValComparator( numHits, fieldname, reversed ) {
				@Override
				public void copy(int slot, int doc) throws IOException {
					super.copy( slot, getNestedDocument( doc ) );
				}
			};
		}

		if ( SortMissingValue.MISSING_LAST.equals( missingValue ) ) {
			return new FieldComparator.TermOrdValComparator( numHits, fieldname, !reversed ) {
				@Override
				public void copy(int slot, int doc) throws IOException {
					super.copy( slot, getNestedDocument( doc ) );
				}
			};
		}

		return new FieldComparator.TermOrdValComparator( numHits, fieldname, reversed ) {
			@Override
			public void copy(int slot, int doc) throws IOException {
				super.copy( slot, getNestedDocument( doc ) );
			}

			@Override
			protected SortedDocValues getSortedDocValues(LeafReaderContext context, String field) throws IOException {
				return new LuceneReplaceMissingSortedDocValues( super.getSortedDocValues( context, field ), (BytesRef) missingValue );
			}
		};
	}
}
