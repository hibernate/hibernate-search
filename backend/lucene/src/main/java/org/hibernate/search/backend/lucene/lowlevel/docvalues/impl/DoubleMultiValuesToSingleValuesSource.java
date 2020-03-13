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
import java.util.function.Function;

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
		return fromField( field, mode, nested, SortedNumericDoubleDocValues::fromDoubleField );
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
		return fromField( field, mode, nested, SortedNumericDoubleDocValues::fromFloatField );
	}

	private static DoubleMultiValuesToSingleValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested,
			Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder) {
		return new FieldMultiValuesToSingleValuesSource( field, mode, nested, decoder );
	}

	protected final MultiValueMode mode;
	protected final NestedDocsProvider nestedDocsProvider;

	public DoubleMultiValuesToSingleValuesSource(MultiValueMode mode, NestedDocsProvider nestedDocsProvider) {
		this.mode = mode;
		this.nestedDocsProvider = nestedDocsProvider;
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
				&& Objects.equals( nestedDocsProvider, that.nestedDocsProvider );
	}

	@Override
	public int hashCode() {
		return Objects.hash( mode, nestedDocsProvider );
	}

	@Override
	public NumericDoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
		SortedNumericDoubleDocValues values = getSortedNumericDoubleDocValues( ctx );

		if ( nestedDocsProvider == null ) {
			return select( values );
		}

		final BitSet rootDocs = nestedDocsProvider.parentDocs( ctx );
		final DocIdSetIterator innerDocs = nestedDocsProvider.childDocs( ctx );
		return select( values, rootDocs, innerDocs, ctx.reader().maxDoc(), Integer.MAX_VALUE );
	}

	/**
	 * Convert to a LongValuesSource by encoding the double values to longs
	 * (using either {@link Double#doubleToRawLongBits(double)} or {@link Float#floatToRawIntBits(float)}).
	 *
	 * @return LongValuesSource
	 */
	public LongValuesSource toRawValuesSource(Function<NumericDoubleValues, NumericDocValues> encoder) {
		return new LongDoubleValuesSource( this, encoder );
	}

	protected abstract SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException;

	protected NumericDoubleValues select(SortedNumericDoubleDocValues values) {
		final NumericDoubleValues singleton = SortedNumericDoubleDocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return singleton;
		}
		else {
			return new NumericDoubleValues() {

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

	protected NumericDoubleValues select(SortedNumericDoubleDocValues values, final BitSet parentDocs,
			final DocIdSetIterator childDocs, int maxDoc, int maxChildren) throws IOException {
		if ( parentDocs == null || childDocs == null ) {
			return NumericDoubleValues.EMPTY;
		}

		JoinFirstChildIdIterator joinIterator = new JoinFirstChildIdIterator( parentDocs, childDocs, values );

		return new NumericDoubleValues() {

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

	protected double pick(SortedNumericDoubleDocValues values) throws IOException {
		final int count = values.docValueCount();
		double result = 0;

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
				// Values are sorted; the first value is the min.
				result = values.nextValue();
				break;
			}
			case MAX: {
				// Values are sorted; the last value is the max.
				for ( int index = 0; index < count - 1; ++index ) {
					values.nextValue();
				}
				result = values.nextValue();
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

		return result;
	}

	protected double pick(SortedNumericDoubleDocValues values, DocIdSetIterator docItr, int startDoc, int endDoc,
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
							returnValue += values.nextValue();
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
							returnValue += values.nextValue();
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
						// Values are sorted; the first value is the min for this document.
						returnValue = Math.min( returnValue, values.nextValue() );
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
						final int docCount = values.docValueCount();
						// Values are sorted; the last value is the max for this document.
						for ( int index = 0; index < docCount - 1; ++index ) {
							values.nextValue();
						}
						returnValue = Math.max( returnValue, values.nextValue() );
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
		private final Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder;

		public FieldMultiValuesToSingleValuesSource(String field, MultiValueMode mode, NestedDocsProvider nestedDocsProvider,
				Function<SortedNumericDocValues, SortedNumericDoubleDocValues> decoder) {
			super( mode, nestedDocsProvider );
			this.field = field;
			this.decoder = decoder;
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
			return Objects.equals( field, that.field )
					&& Objects.equals( decoder, that.decoder );
		}

		@Override
		public int hashCode() {
			return Objects.hash( super.hashCode(), field, decoder );
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
		protected SortedNumericDoubleDocValues getSortedNumericDoubleDocValues(LeafReaderContext ctx) throws IOException {
			// Numeric doc values are longs, but we want doubles
			return decoder.apply( DocValues.getSortedNumeric( ctx.reader(), field ) );
		}
	}

	private static class LongDoubleValuesSource extends LongValuesSource {

		private final DoubleMultiValuesToSingleValuesSource inner;
		private final Function<NumericDoubleValues, NumericDocValues> encoder;

		private LongDoubleValuesSource(DoubleMultiValuesToSingleValuesSource inner,
				Function<NumericDoubleValues, NumericDocValues> encoder) {
			this.inner = inner;
			this.encoder = encoder;
		}

		@Override
		public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			NumericDoubleValues in = inner.getValues( ctx, scores );
			NumericDocValues rawValues = encoder.apply( in );
			return new LongValues() {
				@Override
				public long longValue() throws IOException {
					return rawValues.longValue();
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return rawValues.advanceExact( doc );
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
