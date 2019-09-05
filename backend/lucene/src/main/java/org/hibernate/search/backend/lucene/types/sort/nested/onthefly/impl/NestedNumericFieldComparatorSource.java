/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.onthefly.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.SortedNumericDoubleValues;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BitSet;

public class NestedNumericFieldComparatorSource extends NestedFieldComparatorSource {

	private final SortField.Type type;
	private final Object missingValue;

	public NestedNumericFieldComparatorSource(String nestedDocumentPath, SortField.Type type, Object missingValue) {
		super( nestedDocumentPath );
		this.type = type;
		this.missingValue = missingValue;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		switch ( type ) {
			case INT:
				return new FieldComparator.IntComparator( numHits, fieldname, (Integer) missingValue ) {
					@Override
					protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
						NumericDocValues numericDocValues = super.getNumericDocValues( context, field );
						SortedNumericDocValues sortedNumericDocValues = DocValues.singleton( numericDocValues );

						BitSet parentDocs = docsProvider.parentDocs( context );
						DocIdSetIterator childDocs = docsProvider.childDocs( context );
						if ( parentDocs != null && childDocs != null ) {
							numericDocValues = OnTheFlyNestedSorter.sort( sortedNumericDocValues, missingValue, parentDocs, childDocs );
						}

						return numericDocValues;
					}
				};

			case FLOAT:
				return new FieldComparator.FloatComparator( numHits, fieldname, (Float) missingValue ) {
					@Override
					protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
						NumericDocValues numericDocValues = super.getNumericDocValues( context, field );
						SortedNumericDoubleValues sortedNumericDoubleValues = SortedNumericDoubleValues.createFloat( numericDocValues );

						BitSet parentDocs = docsProvider.parentDocs( context );
						DocIdSetIterator childDocs = docsProvider.childDocs( context );
						if ( parentDocs != null && childDocs != null ) {
							numericDocValues = OnTheFlyNestedSorter.sort( sortedNumericDoubleValues, missingValue, parentDocs, childDocs ).getRawFloatValues();
						}

						return numericDocValues;
					}
				};

			case LONG:
				return new FieldComparator.LongComparator( numHits, fieldname, (Long) missingValue ) {
					@Override
					protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
						NumericDocValues numericDocValues = super.getNumericDocValues( context, field );
						SortedNumericDocValues sortedNumericDocValues = DocValues.singleton( numericDocValues );

						BitSet parentDocs = docsProvider.parentDocs( context );
						DocIdSetIterator childDocs = docsProvider.childDocs( context );
						if ( parentDocs != null && childDocs != null ) {
							numericDocValues = OnTheFlyNestedSorter.sort( sortedNumericDocValues, missingValue, parentDocs, childDocs );
						}

						return numericDocValues;
					}
				};

			case DOUBLE:
				return new FieldComparator.DoubleComparator( numHits, fieldname, (Double) missingValue ) {
					@Override
					protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
						NumericDocValues numericDocValues = super.getNumericDocValues( context, field );
						SortedNumericDoubleValues sortedNumericDoubleValues = SortedNumericDoubleValues.createDouble( numericDocValues );

						BitSet parentDocs = docsProvider.parentDocs( context );
						DocIdSetIterator childDocs = docsProvider.childDocs( context );
						if ( parentDocs != null && childDocs != null ) {
							numericDocValues = OnTheFlyNestedSorter.sort( sortedNumericDoubleValues, missingValue, parentDocs, childDocs ).getRawDoubleValues();
						}

						return numericDocValues;
					}
				};

			default:
				throw new IllegalStateException( "Illegal sort type: " + type );
		}
	}
}
