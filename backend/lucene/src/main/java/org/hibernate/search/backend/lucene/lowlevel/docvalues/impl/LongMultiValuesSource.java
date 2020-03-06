/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.util.BitSet;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

public abstract class LongMultiValuesSource extends LongValuesSource {

	final String field;
	final MultiValueMode mode;

	public LongMultiValuesSource(String field, MultiValueMode mode) {
		this.field = field;
		this.mode = mode;
	}

	/**
	 * Creates a LongMultiValuesSource that wraps a generic NumericDocValues
	 * field
	 *
	 * @param field the field to wrap, must have NumericDocValues
	 * @param mode the multivalue mode
	 * @param nested the nested provider
	 * @return The DoubleMultiValuesSource
	 */
	public static LongMultiValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		if ( nested == null ) {
			return new MultiFieldValuesSource( field, mode );
		}
		else {
			return new NestedMultiFieldValuesSource( field, mode, nested );
		}
	}

	/**
	 * Creates a LongMultiValuesSource that wraps a long-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static LongMultiValuesSource fromLongField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested );
	}

	/**
	 * Creates a LongMultiValuesSource that wraps an int-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static LongMultiValuesSource fromIntField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromLongField( field, mode, nested );
	}

	/**
	 * Returns a {@link NumericDocValues} instance for the passed-in LeafReaderContext and scores
	 *
	 * If scores are not needed to calculate the values (ie {@link #needsScores() returns false}, callers
	 * may safely pass {@code null} for the {@code scores} parameter.
	 *
	 * @param ctx the ctx
	 * @param scores the scores
	 * @return NumericDocValues
	 * @throws java.io.IOException
	 */
	public NumericDocValues getRawNumericDocValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
		return new RawNumericDocValues( getValues( ctx, scores ) );
	}

	protected LongValues select(final SortedNumericDocValues values, final DoubleValues scores) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return replaceScores( new LongValues() {

				@Override
				public long longValue() throws IOException {
					return singleton.longValue();
				}

				@Override
				public boolean advanceExact(int target) throws IOException {
					return singleton.advanceExact( target );
				}
			}, scores );
		}
		else {
			return new LongValues() {

				private long value;

				@Override
				public long longValue() throws IOException {
					return value;
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					if ( values.advanceExact( doc ) ) {
						value = pick( values, scores, doc );
						return true;
					}
					else if ( scores != null && scores.advanceExact( doc ) ) {
						value = (long) scores.doubleValue();
					}
					return false;
				}
			};
		}
	}

	protected LongValues select(final SortedNumericDocValues values, final DoubleValues scores, final BitSet parentDocs,
		final DocIdSetIterator childDocs, int maxDoc, int maxChildren) throws IOException {
		if ( parentDocs == null || childDocs == null ) {
			return replaceScores( DoubleValues.EMPTY, scores );
		}

		return new LongValues() {

			int lastSeenParentDoc = -1;
			long lastEmittedValue = -1;

			@Override
			public long longValue() throws IOException {
				return lastEmittedValue;
			}

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return true;
				}
				else if ( parentDoc == 0 ) {
					if ( scores != null && scores.advanceExact( parentDoc ) ) {
						lastEmittedValue = (long) scores.doubleValue();
						return true;
					}
					else {
						return false;
					}
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
				lastEmittedValue = pick( values, scores, childDocs, firstChildDoc, parentDoc, maxChildren );
				return true;
			}

		};
	}

	protected long pick(SortedNumericDocValues values, DoubleValues scores, int doc) throws IOException {
		final int count = values.docValueCount();
		long result = 0;

		switch ( mode ) {
			case SUM: {
				for ( int index = 0; index < count; ++index ) {
					result += values.nextValue();
				}
				break;
			}
			case AVG: {
				for ( int index = 0; index < count; ++index ) {
					result += values.nextValue();
				}
				result = result / count;
				break;
			}
			case MIN: {
				result = Long.MAX_VALUE;
				for ( int index = 0; index < count; ++index ) {
					result = Math.min( result, values.nextValue() );
				}
				break;
			}
			case MAX: {
				result = Long.MIN_VALUE;
				for ( int index = 0; index < count; ++index ) {
					result = Math.max( result, values.nextValue() );
				}
				break;
			}
			case MEDIAN: {
				for ( int i = 0; i < (count - 1) / 2; ++i ) {
					values.nextValue();
				}
				if ( count % 2 == 0 ) {
					result = (values.nextValue() + values.nextValue()) / 2;
				}
				else {
					result = values.nextValue();
				}
				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		if ( count == 0 && scores != null && scores.advanceExact( doc ) ) {
			result = (long) scores.doubleValue();
		}

		return result;
	}

	protected long pick(SortedNumericDocValues values, DoubleValues scores, DocIdSetIterator docItr, int startDoc, int endDoc,
		int maxChildren) throws IOException {
		boolean hasValue = false;
		int totalCount = 0;
		long returnValue = 0;

		switch ( mode ) {
			case SUM: {
				int count = 0;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						final int docCount = values.docValueCount();
						for ( int index = 0; index < docCount; ++index ) {
							returnValue += values.nextValue();
						}
						totalCount += docCount;
						hasValue = true;
					}
				}
				break;
			}
			case AVG: {
				int count = 0;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						final int docCount = values.docValueCount();
						for ( int index = 0; index < docCount; ++index ) {
							returnValue += values.nextValue();
						}
						totalCount += docCount;
						hasValue = true;
					}
				}
				if ( totalCount > 0 ) {
					returnValue = returnValue / totalCount;
				}
				else {
					returnValue = 0;
				}
				break;
			}
			case MIN: {
				returnValue = Long.MAX_VALUE;
				int count = 0;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.min( returnValue, values.nextValue() );
						hasValue = true;
					}
				}
				break;
			}
			case MAX: {
				returnValue = Long.MIN_VALUE;
				int count = 0;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.max( returnValue, values.nextValue() );
						hasValue = true;
					}
				}
				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		if ( !hasValue && scores != null && scores.advanceExact( endDoc ) ) {
			returnValue = (long) scores.doubleValue();
		}

		return returnValue;

	}

	protected LongValues replaceScores(LongValues values, DoubleValues scores) {
		return new LongValues() {

			private long value;

			@Override
			public boolean advanceExact(int target) throws IOException {
				if ( values.advanceExact( target ) ) {
					value = values.longValue();
				}
				else if ( scores != null && scores.advanceExact( target ) ) {
					value = (long) scores.doubleValue();
				}
				return false;
			}

			@Override
			public long longValue() throws IOException {
				return value;
			}
		};
	}

	protected LongValues replaceScores(DoubleValues values, DoubleValues scores) {
		return new LongValues() {

			private double value;

			@Override
			public boolean advanceExact(int target) throws IOException {
				if ( values.advanceExact( target ) ) {
					value = values.doubleValue();
				}
				else if ( scores != null && scores.advanceExact( target ) ) {
					value = scores.doubleValue();
				}
				return false;
			}

			@Override
			public long longValue() throws IOException {
				return (long) value;
			}
		};
	}

	private static class MultiFieldValuesSource extends LongMultiValuesSource {

		private MultiFieldValuesSource(String field, MultiValueMode mode) {
			super( field, mode );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			NestedMultiFieldValuesSource that = (NestedMultiFieldValuesSource) o;
			return Objects.equals( field, that.field );
		}

		@Override
		public String toString() {
			return "long(" + field + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash( field );
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			SortedNumericDocValues values = DocValues.getSortedNumeric( ctx.reader(), field );
			return select( values, scores );
		}

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return DocValues.isCacheable( ctx, field );
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return this;
		}

	}

	private static class NestedMultiFieldValuesSource extends LongMultiValuesSource {

		private final NestedDocsProvider nested;

		private NestedMultiFieldValuesSource(String field, MultiValueMode mode, NestedDocsProvider nested) {
			super( field, mode );
			this.nested = nested;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			NestedMultiFieldValuesSource that = (NestedMultiFieldValuesSource) o;
			return Objects.equals( field, that.field );
		}

		@Override
		public String toString() {
			return "long(" + field + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash( field );
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			SortedNumericDocValues values = DocValues.getSortedNumeric( ctx.reader(), field );

			if ( scores == null ) {
				scores = DoubleValues.EMPTY;
			}

			final BitSet rootDocs = nested.parentDocs( ctx );
			final DocIdSetIterator innerDocs = nested.childDocs( ctx );
			return select( values, scores, rootDocs, innerDocs, ctx.reader().maxDoc(), Integer.MAX_VALUE );

		}

		@Override
		public boolean needsScores() {
			return false;
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return DocValues.isCacheable( ctx, field );
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return this;
		}

	}

	private static class RawNumericDocValues extends NumericDocValues {
		private int docID = -1;
		private final LongValues values;

		public RawNumericDocValues(LongValues values) {
			this.values = values;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return values.advanceExact( target );
		}

		@Override
		public long longValue() throws IOException {
			return values.longValue();
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
	}

}
