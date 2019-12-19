/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;
import org.hibernate.search.util.common.impl.Contracts;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BytesRef;

/**
 * Utils around docvalues and nested documents.
 * <p>
 * Copied and adapted from {@code org.elasticsearch.search.MultiValueMode} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch project</a>.
 */
public class DocValuesJoin {

	private DocValuesJoin() {
	}

	public static SortedDocValues getJoinedAsSingleValuedSorted(LeafReaderContext context, String field,
			NestedDocsProvider nestedDocsProvider) throws IOException {
		SortedDocValues sortedDocValues = DocValues.getSorted( context.reader(), field );
		if ( nestedDocsProvider == null ) {
			// No join requested
			return sortedDocValues;
		}

		BitSet parentDocs = nestedDocsProvider.parentDocs( context );
		DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
		if ( parentDocs == null || childDocs == null ) {
			// No join possible
			return sortedDocValues;
		}

		return joinAsSingleValued( sortedDocValues, parentDocs, childDocs );
	}

	public static NumericDocValues getJoinedAsSingleValuedNumeric(LeafReaderContext context, String field,
			NestedDocsProvider nestedDocsProvider, long missingValue) throws IOException {
		NumericDocValues numericDocValues = DocValues.getNumeric( context.reader(), field );
		if ( nestedDocsProvider == null ) {
			// No join requested
			return numericDocValues;
		}

		SortedNumericDocValues sortedNumericDocValues = DocValues.singleton( numericDocValues );
		BitSet parentDocs = nestedDocsProvider.parentDocs( context );
		DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
		if ( parentDocs == null || childDocs == null ) {
			// No join possible
			return numericDocValues;
		}

		return joinAsSingleValued( sortedNumericDocValues, missingValue, parentDocs, childDocs );
	}

	public static NumericDocValues getJoinedAsSingleValuedNumericDouble(LeafReaderContext context, String field,
			NestedDocsProvider nestedDocsProvider, double missingValue) throws IOException {
		NumericDocValues numericDocValues = DocValues.getNumeric( context.reader(), field );
		if ( nestedDocsProvider == null ) {
			// No join requested
			return numericDocValues;
		}

		SortedNumericDoubleValues sortedNumericDoubleValues =
				SortedNumericDoubleValues.createDouble( numericDocValues );
		BitSet parentDocs = nestedDocsProvider.parentDocs( context );
		DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
		if ( parentDocs == null || childDocs == null ) {
			// No join possible
			return numericDocValues;
		}

		return joinAsSingleValued( sortedNumericDoubleValues, missingValue, parentDocs, childDocs )
				.getRawDoubleValues();
	}

	public static NumericDocValues getJoinedAsSingleValuedNumericFloat(LeafReaderContext context, String field,
			NestedDocsProvider nestedDocsProvider, float missingValue) throws IOException {
		NumericDocValues numericDocValues = DocValues.getNumeric( context.reader(), field );
		if ( nestedDocsProvider == null ) {
			// No join requested
			return numericDocValues;
		}

		SortedNumericDoubleValues sortedNumericDoubleValues = SortedNumericDoubleValues.createFloat( numericDocValues );
		BitSet parentDocs = nestedDocsProvider.parentDocs( context );
		DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
		if ( parentDocs == null || childDocs == null ) {
			// No join possible
			return numericDocValues;
		}

		return joinAsSingleValued( sortedNumericDoubleValues, missingValue, parentDocs, childDocs )
				.getRawFloatValues();
	}

	public static NumericDoubleValues getJoinedAsSingleValuedDistance(LeafReaderContext context, String field,
			NestedDocsProvider nestedDocsProvider, double centerLatitude, double centerLongitude,
			double missingValue) throws IOException {
		SortedNumericDocValues sortedNumericDocValues = context.reader().getSortedNumericDocValues( field );
		GeoPointDistanceDocValues geoPointDistanceDocValues =
				new GeoPointDistanceDocValues( sortedNumericDocValues, centerLatitude, centerLongitude );
		if ( nestedDocsProvider == null ) {
			// No join requested
			return geoPointDistanceDocValues;
		}

		BitSet parentDocs = nestedDocsProvider.parentDocs( context );
		DocIdSetIterator childDocs = nestedDocsProvider.childDocs( context );
		if ( parentDocs == null || childDocs == null ) {
			// No join possible
			return geoPointDistanceDocValues;
		}

		SortedNumericDoubleValues sortedNumericDoubleValues =
				SortedNumericDoubleValues.createDistance( geoPointDistanceDocValues );
		return joinAsSingleValued( sortedNumericDoubleValues, missingValue, parentDocs, childDocs );
	}

	private static SortedDocValues joinAsSingleValued(SortedDocValues selectedValues, BitSet parentDocs, DocIdSetIterator childDocs) {
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

	private static NumericDocValues joinAsSingleValued(SortedNumericDocValues values, long missingValue,
			BitSet parentDocs, DocIdSetIterator childDocs) {
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

	private static NumericDoubleValues joinAsSingleValued(SortedNumericDoubleValues values, double missingValue, BitSet parentDocs, DocIdSetIterator childDocs) {
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
	private static int pick(SortedDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc, int maxChildren) throws IOException {
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
	private static long pick(SortedNumericDocValues values, long missingValue, DocIdSetIterator docItr, int startDoc, int endDoc,
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
	private static double pick(SortedNumericDoubleValues values, double missingValue, DocIdSetIterator docItr, int startDoc, int endDoc,
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
