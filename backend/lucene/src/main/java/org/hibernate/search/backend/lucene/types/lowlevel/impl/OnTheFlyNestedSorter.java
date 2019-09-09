/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import java.io.IOException;

import org.hibernate.search.util.common.impl.Contracts;

import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BytesRef;

/**
 * Allows to sort on nested document field values.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.search.MultiValueMode} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public class OnTheFlyNestedSorter {

	private OnTheFlyNestedSorter() {
	}

	public static SortedDocValues sort(SortedDocValues selectedValues, BitSet parentDocs, DocIdSetIterator childDocs) {
		Contracts.assertNotNull( parentDocs, "parent docs" );
		Contracts.assertNotNull( childDocs, "child docs" );

		return new SortedDocValues() {

			int docID = -1;
			int lastSeenParentDoc = 0;
			int lastEmittedOrd = -1;

			@Override
			public BytesRef lookupOrd(int ord) throws IOException {
				return selectedValues.lookupOrd( ord );
			}

			@Override
			public int getValueCount() {
				return selectedValues.getValueCount();
			}

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming root docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return lastEmittedOrd != -1;
				}

				final int prevParentDoc = parentDocs.prevSetBit( parentDoc - 1 );
				final int firstChildDoc;
				if ( childDocs.docID() > prevParentDoc ) {
					firstChildDoc = childDocs.docID();
				}
				else {
					firstChildDoc = childDocs.advance( prevParentDoc + 1 );
				}

				docID = lastSeenParentDoc = parentDoc;
				lastEmittedOrd = pick( selectedValues, childDocs, firstChildDoc, parentDoc, Integer.MAX_VALUE );
				return lastEmittedOrd != -1;
			}

			@Override
			public int docID() {
				return docID;
			}

			@Override
			public int nextDoc() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int advance(int target) {
				throw new UnsupportedOperationException();
			}

			@Override
			public long cost() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int ordValue() {
				return lastEmittedOrd;
			}
		};
	}

	public static NumericDocValues sort(SortedNumericDocValues values, long missingValue, BitSet parentDocs, DocIdSetIterator childDocs) {

		return new NumericDocValues() {

			int lastSeenParentDoc = -1;
			long lastEmittedValue = missingValue;

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return true;
				}
				else if ( parentDoc == 0 ) {
					lastEmittedValue = missingValue;
					return true;
				}
				final int prevParentDoc = parentDocs.prevSetBit( parentDoc - 1 );
				final int firstChildDoc;
				if ( childDocs.docID() > prevParentDoc ) {
					firstChildDoc = childDocs.docID();
				}
				else {
					firstChildDoc = childDocs.advance( prevParentDoc + 1 );
				}

				lastSeenParentDoc = parentDoc;
				lastEmittedValue = pick( values, missingValue, childDocs, firstChildDoc, parentDoc, Integer.MAX_VALUE );
				return true;
			}

			@Override
			public int docID() {
				return lastSeenParentDoc;
			}

			@Override
			public int nextDoc() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int advance(int target) {
				throw new UnsupportedOperationException();
			}

			@Override
			public long cost() {
				throw new UnsupportedOperationException();
			}

			@Override
			public long longValue() {
				return lastEmittedValue;
			}
		};
	}

	public static NumericDoubleValues sort(SortedNumericDoubleValues values, double missingValue, BitSet parentDocs, DocIdSetIterator childDocs) {

		return new NumericDoubleValues() {

			int lastSeenParentDoc = 0;
			double lastEmittedValue = missingValue;

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return true;
				}
				final int prevParentDoc = parentDocs.prevSetBit( parentDoc - 1 );
				final int firstChildDoc;
				if ( childDocs.docID() > prevParentDoc ) {
					firstChildDoc = childDocs.docID();
				}
				else {
					firstChildDoc = childDocs.advance( prevParentDoc + 1 );
				}

				lastSeenParentDoc = parentDoc;
				lastEmittedValue = pick( values, missingValue, childDocs, firstChildDoc, parentDoc, Integer.MAX_VALUE );
				return true;
			}

			@Override
			public double doubleValue() {
				return lastEmittedValue;
			}
		};
	}

	// This method has been taken from MultiValueMode.MIN.
	// We could have take the one from MultiValueMode.MAX.
	// Since for single value sorting they are equivalent.
	protected static int pick(SortedDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc, int maxChildren) throws IOException {
		int ord = Integer.MAX_VALUE;
		boolean hasValue = false;
		int count = 0;
		for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
			if ( values.advanceExact( doc ) ) {
				if ( ++count > maxChildren ) {
					break;
				}
				final int innerOrd = values.ordValue();
				ord = Math.min( ord, innerOrd );
				hasValue = true;
			}
		}

		return hasValue ? ord : -1;
	}

	// This method has been taken from MultiValueMode.MIN.
	// We could have take the other ones.
	// Since for single value sorting they are all equivalent.
	protected static long pick(SortedNumericDocValues values, long missingValue, DocIdSetIterator docItr, int startDoc, int endDoc,
			int maxChildren) throws IOException {
		boolean hasValue = false;
		long minValue = Long.MAX_VALUE;
		int count = 0;
		for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
			if ( values.advanceExact( doc ) ) {
				if ( ++count > maxChildren ) {
					break;
				}
				minValue = Math.min( minValue, values.nextValue() );
				hasValue = true;
			}
		}
		return hasValue ? minValue : missingValue;
	}

	// This method has been taken from MultiValueMode.MIN.
	// We could have take the other ones.
	// Since for single value sorting they are all equivalent.
	protected static double pick(SortedNumericDoubleValues values, double missingValue, DocIdSetIterator docItr, int startDoc, int endDoc,
			int maxChildren) throws IOException {
		boolean hasValue = false;
		double minValue = Double.POSITIVE_INFINITY;
		int count = 0;
		for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
			if ( values.advanceExact( doc ) ) {
				if ( ++count > maxChildren ) {
					break;
				}
				minValue = Math.min( minValue, values.nextValue() );
				hasValue = true;
			}
		}
		return hasValue ? minValue : missingValue;
	}
}
