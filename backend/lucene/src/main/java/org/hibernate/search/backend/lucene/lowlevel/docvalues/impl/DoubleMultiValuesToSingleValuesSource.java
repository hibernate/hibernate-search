/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.Objects;
import java.util.function.DoubleToLongFunction;
import java.util.function.LongToDoubleFunction;

import org.hibernate.search.backend.lucene.lowlevel.join.impl.JoinFirstChildIdIterator;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.util.BitSet;

/**
 * An implementation of {@link DoubleValuesSource} for docvalues with multiple values per document,
 * where multiple values are "aggregated" into a single value
 * according to a given {@link MultiValueMode}.
 */
public abstract class DoubleMultiValuesToSingleValuesSource extends DoubleValuesSource {

	/**
	 * Creates a {@link DoubleMultiValuesToSingleValuesSource} that wraps a double-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link DoubleMultiValuesToSingleValuesSource}
	 */
	public static DoubleMultiValuesToSingleValuesSource fromDoubleField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested,
				Double::longBitsToDouble, Double::doubleToRawLongBits );
	}

	/**
	 * Creates a {@link DoubleMultiValuesToSingleValuesSource} that wraps a float-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return A {@link DoubleMultiValuesToSingleValuesSource}
	 */
	public static DoubleMultiValuesToSingleValuesSource fromFloatField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested,
				(v) -> (double) Float.intBitsToFloat( (int) v ), (v) -> (long) Float.floatToRawIntBits( (float) v ) );
	}

	private static DoubleMultiValuesToSingleValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested,
			LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
		return new FieldMultiValuesToSingleValuesSource( field, mode, nested, decoder, encoder );
	}

	protected final MultiValueMode mode;
	protected final NestedDocsProvider nestedDocsProvider;
	private final DoubleToLongFunction encoder;
	private final LongToDoubleFunction decoder;

	public DoubleMultiValuesToSingleValuesSource(MultiValueMode mode, NestedDocsProvider nestedDocsProvider,
			LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
		this.mode = mode;
		this.nestedDocsProvider = nestedDocsProvider;
		this.decoder = decoder;
		this.encoder = encoder;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DoubleMultiValuesToSingleValuesSource that = (DoubleMultiValuesToSingleValuesSource) o;
		return Objects.equals( mode, that.mode )
				&& Objects.equals( nestedDocsProvider, that.nestedDocsProvider )
				&& Objects.equals( encoder, that.encoder )
				&& Objects.equals( decoder, that.decoder );
	}

	@Override
	public int hashCode() {
		return Objects.hash( mode, nestedDocsProvider, encoder, decoder );
	}

	@Override
	public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
		SortedNumericDocValues values = getSortedNumericDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return select( values );
		}

		final BitSet rootDocs = nestedDocsProvider.parentDocs( ctx );
		final DocIdSetIterator innerDocs = nestedDocsProvider.childDocs( ctx );
		return select( values, rootDocs, innerDocs, ctx.reader().maxDoc(), Integer.MAX_VALUE );
	}

	/**
	 * Returns a {@link NumericDocValues} instance for the passed-in LeafReaderContext
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
		return new RawNumericDocValues( getValues( ctx, scores ), encoder );
	}

	/**
	 * Convert to a LongValuesSource by casting the double values to longs
	 *
	 * @return LongValuesSource
	 */
	public LongValuesSource getLongValuesSource() {
		return new LongDoubleValuesSource( this, encoder );
	}

	protected abstract SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException;

	private double decode(long value) {
		return decoder.applyAsDouble( value );
	}

	protected DoubleValues select(final SortedNumericDocValues values) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return new DoubleValues() {
				@Override
				public double doubleValue() throws IOException {
					return decode( singleton.longValue() );
				}

				@Override
				public boolean advanceExact(int target) throws IOException {
					return singleton.advanceExact( target );
				}
			};
		}
		else {
			return new DoubleValues() {

				private double value;

				@Override
				public double doubleValue() throws IOException {
					return value;
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					if ( values.advanceExact( doc ) ) {
						value = pick( values );
						return true;
					}
					return false;
				}
			};
		}
	}

	protected DoubleValues select(final SortedNumericDocValues values, final BitSet parentDocs,
			final DocIdSetIterator childDocs, int maxDoc, int maxChildren) throws IOException {
		if ( parentDocs == null || childDocs == null ) {
			return DoubleValues.EMPTY;
		}

		JoinFirstChildIdIterator joinIterator = new JoinFirstChildIdIterator( parentDocs, childDocs, values );

		return new DoubleValues() {

			int lastSeenParentDoc = -1;
			double lastEmittedValue = -1;

			@Override
			public double doubleValue() throws IOException {
				return lastEmittedValue;
			}

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					return true;
				}

				int nextChildWithValue = joinIterator.advance( parentDoc );
				if ( nextChildWithValue == JoinFirstChildIdIterator.NO_CHILD_WITH_VALUE ) {
					// No child of this parent has a value
					return false;
				}

				lastSeenParentDoc = parentDoc;
				lastEmittedValue = pick( values, childDocs, nextChildWithValue, parentDoc, maxChildren );
				return true;
			}

		};
	}

	protected double pick(SortedNumericDocValues values) throws IOException {
		final int count = values.docValueCount();
		double result = 0;

		switch ( mode ) {
			case SUM: {
				for ( int index = 0; index < count; ++index ) {
					result += decode( values.nextValue() );
				}
				break;
			}
			case AVG: {
				for ( int index = 0; index < count; ++index ) {
					result += decode( values.nextValue() );
				}
				result = result / count;
				break;
			}
			case MIN: {
				result = Double.POSITIVE_INFINITY;
				for ( int index = 0; index < count; ++index ) {
					result = Math.min( result, decode( values.nextValue() ) );
				}
				break;
			}
			case MAX: {
				result = Double.NEGATIVE_INFINITY;
				for ( int index = 0; index < count; ++index ) {
					result = Math.max( result, decode( values.nextValue() ) );
				}
				break;
			}
			case MEDIAN: {
				for ( int i = 0; i < (count - 1) / 2; ++i ) {
					values.nextValue();
				}
				if ( count % 2 == 0 ) {
					result = (decode( values.nextValue() ) + decode( values.nextValue() )) / 2;
				}
				else {
					result = decode( values.nextValue() );
				}
				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		return result;
	}

	protected double pick(SortedNumericDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc,
		int maxChildren) throws IOException {
		int totalCount = 0;
		double returnValue = 0;

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
							returnValue += decode( values.nextValue() );
						}
						totalCount += docCount;
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
							returnValue += decode( values.nextValue() );
						}
						totalCount += docCount;
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
				returnValue = Double.POSITIVE_INFINITY;
				int count = 0;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.min( returnValue, decode( values.nextValue() ) );
					}
				}
				break;
			}
			case MAX: {
				returnValue = Double.NEGATIVE_INFINITY;
				int count = 0;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.max( returnValue, decode( values.nextValue() ) );
					}
				}
				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		return returnValue;
	}

	private static class FieldMultiValuesToSingleValuesSource extends DoubleMultiValuesToSingleValuesSource {

		private final String field;

		public FieldMultiValuesToSingleValuesSource(String field, MultiValueMode mode, NestedDocsProvider nestedDocsProvider,
				LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
			super( mode, nestedDocsProvider, decoder, encoder );
			this.field = field;
		}

		@Override
		public String toString() {
			return "double(" + field + "," + mode + "," + nestedDocsProvider + ")";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !super.equals( o ) ) {
				return false;
			}
			FieldMultiValuesToSingleValuesSource that = (FieldMultiValuesToSingleValuesSource) o;
			return Objects.equals( field, that.field );
		}

		@Override
		public int hashCode() {
			return Objects.hash( super.hashCode(), field );
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
		public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
			DoubleValues values = getValues( ctx, null );
			if ( values.advanceExact( docId ) ) {
				return Explanation.match( values.doubleValue(), this.toString() );
			}
			else {
				return Explanation.noMatch( this.toString() );
			}
		}

		@Override
		public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return this;
		}

		@Override
		protected SortedNumericDocValues getSortedNumericDocValues(LeafReaderContext ctx) throws IOException {
			return DocValues.getSortedNumeric( ctx.reader(), field );
		}
	}

	private static class RawNumericDocValues extends NumericDocValues {
		private int docID = -1;
		private final DoubleValues values;
		private final DoubleToLongFunction encoder;

		public RawNumericDocValues(DoubleValues values, DoubleToLongFunction encoder) {
			this.values = values;
			this.encoder = encoder;
		}

		@Override
		public boolean advanceExact(int target) throws IOException {
			docID = target;
			return values.advanceExact( target );
		}

		@Override
		public long longValue() throws IOException {
			return encoder.applyAsLong( values.doubleValue() );
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

	private static class LongDoubleValuesSource extends LongValuesSource {

		private final DoubleValuesSource inner;
		private final DoubleToLongFunction encoder;

		private LongDoubleValuesSource(DoubleValuesSource inner, DoubleToLongFunction encoder) {
			this.inner = inner;
			this.encoder = encoder;
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			DoubleValues in = inner.getValues( ctx, scores );
			return new LongValues() {
				@Override
				public long longValue() throws IOException {
					return encoder.applyAsLong( in.doubleValue() );
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return in.advanceExact( doc );
				}
			};
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return inner.isCacheable( ctx );
		}

		@Override
		public boolean needsScores() {
			return inner.needsScores();
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			LongDoubleValuesSource that = (LongDoubleValuesSource) o;
			return Objects.equals( inner, that.inner );
		}

		@Override
		public int hashCode() {
			return Objects.hash( inner );
		}

		@Override
		public String toString() {
			return "long(" + inner.toString() + ")";
		}

		@Override
		public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return inner.rewrite( searcher ).toLongValuesSource();
		}

	}

}
