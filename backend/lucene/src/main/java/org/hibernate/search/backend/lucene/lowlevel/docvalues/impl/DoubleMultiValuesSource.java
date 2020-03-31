/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.DoubleToLongFunction;
import java.util.function.LongToDoubleFunction;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.util.BitSet;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

public abstract class DoubleMultiValuesSource extends DoubleValuesSource {

	protected final String field;
	protected final MultiValueMode mode;
	protected final LongToDoubleFunction decoder;
	protected final DoubleToLongFunction encoder;

	public DoubleMultiValuesSource(String field, MultiValueMode mode,
		LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
		this.field = field;
		this.mode = mode;
		this.decoder = decoder;
		this.encoder = encoder;
	}

	/**
	 * Returns a {@link DoubleValues} instance for the passed-in LeafReaderContext and scores
	 *
	 * If scores are not needed to calculate the values (ie {@link #needsScores() returns false}, callers
	 * may safely pass {@code null} for the {@code scores} parameter.
	 *
	 * @param ctx
	 * @param scores
	 * @return
	 * @throws java.io.IOException
	 */
	@Override
	public abstract DoubleMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException;

	/**
	 * Creates a DoubleMultiValuesSource that wraps a generic NumericDocValues
	 * field
	 *
	 * @param field the field to wrap, must have NumericDocValues
	 * @param mode the multivalue mode
	 * @param nested the nested provider
	 * @param decoder a function to convert the long-valued doc values to doubles
	 * @param encoder a function to convert the double-valued doc values to long
	 * @return The DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromField(String field, MultiValueMode mode, NestedDocsProvider nested,
		LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
		if ( nested == null ) {
			return new MultiFieldValuesSource( field, mode, decoder, encoder );
		}
		else {
			return new NestedMultiFieldValuesSource( field, mode, nested, decoder, encoder );
		}
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps a double-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromDoubleField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested,
			Double::longBitsToDouble, Double::doubleToRawLongBits );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps a double-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromDoubleField(String field, MultiValueMode mode) {
		return fromDoubleField( field, mode, null );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps a float-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromFloatField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested,
			(v) -> (double) Float.intBitsToFloat( (int) v ), (v) -> (long) Float.floatToRawIntBits( (float) v ) );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps a float-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromFloatField(String field, MultiValueMode mode) {
		return fromFloatField( field, mode, null );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps a long-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromLongField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromField( field, mode, nested,
			(v) -> (double) v, (v) -> (long) v );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps a long-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromLongField(String field, MultiValueMode mode) {
		return fromLongField( field, mode, null );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps an int-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @param nested the nested provider
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromIntField(String field, MultiValueMode mode, NestedDocsProvider nested) {
		return fromLongField( field, mode, nested );
	}

	/**
	 * Creates a DoubleMultiValuesSource that wraps an int-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @return DoubleMultiValuesSource
	 */
	public static DoubleMultiValuesSource fromIntField(String field, MultiValueMode mode) {
		return fromIntField( field, mode, null );
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
		return new RawNumericDocValues( getValues( ctx, scores ), encoder );
	}

	/**
	 * Convert to a LongValuesSource by casting the double values to longs
	 *
	 * @return LongValuesSource
	 */
	public LongMultiValuesSource getLongValuesSource() {
		return new LongDoubleValuesSource( this, field, mode, encoder );
	}

	protected double decode(long value) {
		return decoder.applyAsDouble( value );
	}

	protected float encode(double value) {
		return encoder.applyAsLong( value );
	}

	protected DoubleMultiValues select(final SortedNumericDocValues values, final DoubleValues scores) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return replaceScores( new DoubleMultiValues() {
				@Override
				public double doubleValue() throws IOException {
					return decode( singleton.longValue() );
				}

				@Override
				public boolean advanceExact(int target) throws IOException {
					return singleton.advanceExact( target );
				}
			}, scores );
		}
		else if ( mode == null || mode == MultiValueMode.NONE ) {
			return replaceScores( new DoubleMultiValues() {

				@Override
				public double doubleValue() throws IOException {
					return decode( values.nextValue() );
				}

				@Override
				public int docValueCount() {
					return values.docValueCount();
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return values.advanceExact( doc );
				}
			}, scores );
		}
		else {
			return new DoubleMultiValues() {
				private OptionalDouble value = OptionalDouble.empty();

				@Override
				public double doubleValue() throws IOException {
					return value.getAsDouble();
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					value = OptionalDouble.empty();
					if ( values.advanceExact( doc ) ) {
						value = pick( values, scores, doc );
						return true;
					}
					else if ( scores != null && scores.advanceExact( doc ) ) {
						value = OptionalDouble.of( scores.doubleValue() );
					}
					return false;
				}
			};
		}
	}

	protected DoubleMultiValues select(final SortedNumericDocValues values, final DoubleValues scores, final BitSet parentDocs,
		final DocIdSetIterator childDocs, int maxDoc, int maxChildren) throws IOException {
		if ( parentDocs == null || childDocs == null ) {
			return replaceScores( DoubleValues.EMPTY, scores );
		}

		return new DoubleMultiValues() {
			int lastSeenParentDoc = -1;
			double lastEmittedValue = -1;
			private Iterator<Double> value;
			private List<Double> all;

			@Override
			public double doubleValue() throws IOException {
				return value != null ? value.next() : -1;
			}

			@Override
			public int docValueCount() {
				return all != null ? all.size() : -1;
			}

			@Override
			public boolean advanceExact(int parentDoc) throws IOException {
				assert parentDoc >= lastSeenParentDoc : "can only evaluate current and upcoming parent docs";
				if ( parentDoc == lastSeenParentDoc ) {
					value = all.iterator();
					return true;
				}
				else if ( parentDoc == 0 ) {
					all = null;
					value = null;
					if ( scores != null && scores.advanceExact( parentDoc ) ) {
						all = Collections.singletonList( scores.doubleValue() );
						value = all.iterator();
						return true;
					}
					else {
						return false;
					}
				}

				all = null;
				value = null;

				final int prevParentDoc = parentDocs.prevSetBit( parentDoc - 1 );
				final int firstChildDoc;
				if ( childDocs.docID() > prevParentDoc ) {
					firstChildDoc = childDocs.docID();
				}
				else {
					firstChildDoc = childDocs.advance( prevParentDoc + 1 );
				}

				lastSeenParentDoc = parentDoc;

				if ( mode == null || mode == MultiValueMode.NONE ) {
					all = list( values, scores, childDocs, firstChildDoc, parentDoc, maxChildren );
				}
				else {
					OptionalDouble pick = pick( values, scores, childDocs, firstChildDoc, parentDoc, maxChildren );
					if ( pick.isPresent() ) {
						all = Collections.singletonList( pick.getAsDouble() );
					}
					else if ( scores != null && scores.advanceExact( parentDoc ) ) {
						all = Collections.singletonList( scores.doubleValue() );
					}
				}

				if ( all != null && !all.isEmpty() ) {
					value = all.iterator();
					return true;
				}

				return false;
			}
		};
	}

	protected OptionalDouble pick(SortedNumericDocValues values, DoubleValues scores, int doc) throws IOException {
		final int count = values.docValueCount();
		OptionalDouble result = OptionalDouble.empty();

		switch ( mode ) {
			case SUM: {
				boolean hasValue = false;
				double returnValue = 0;
				for ( int index = 0; index < count; ++index ) {
					returnValue += decode( values.nextValue() );
					hasValue = true;
				}

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case AVG: {
				boolean hasValue = false;
				double returnValue = 0;
				for ( int index = 0; index < count; ++index ) {
					returnValue += decode( values.nextValue() );
					hasValue = true;
				}
				returnValue = returnValue / count;

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case MIN: {
				boolean hasValue = true;
				double returnValue = values.nextValue();
				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case MAX: {
				boolean hasValue = true;
				final int valueCount = values.docValueCount();
				// Values are sorted; the last value is the max.
				for ( int index = 0; index < valueCount - 1; ++index ) {
					values.nextValue();
				}
				double returnValue = values.nextValue();

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case MEDIAN: {
				boolean hasValue = false;
				double returnValue = 0;

				if ( count > 0 ) {
					for ( int i = 0; i < (count - 1) / 2; ++i ) {
						values.nextValue();
					}
					if ( count % 2 == 0 ) {
						returnValue = (decode( values.nextValue() ) + decode( values.nextValue() )) / 2;
						hasValue = true;
					}
					else {
						returnValue = decode( values.nextValue() );
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		if ( !result.isPresent() && scores.advanceExact( doc ) ) {
			result = OptionalDouble.of( scores.doubleValue() );

		}

		return result;
	}

	protected OptionalDouble pick(SortedNumericDocValues values, DoubleValues scores, DocIdSetIterator docItr, int startDoc, int endDoc,
		int maxChildren) throws IOException {

		OptionalDouble result = OptionalDouble.empty();

		switch ( mode ) {
			case SUM: {
				int count = 0;
				double returnValue = 0;
				boolean hasValue = false;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						final int docCount = values.docValueCount();
						for ( int index = 0; index < docCount; ++index ) {
							returnValue += decode( values.nextValue() );
						}
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case AVG: {
				int count = 0;
				int totalCount = 0;
				double returnValue = 0;
				boolean hasValue = false;
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
						hasValue = true;
					}
				}
				if ( totalCount > 0 ) {
					returnValue = returnValue / totalCount;
				}
				else {
					returnValue = 0;
				}

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case MIN: {
				double returnValue = Double.POSITIVE_INFINITY;
				int count = 0;
				boolean hasValue = false;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.min( returnValue, decode( values.nextValue() ) );
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case MAX: {
				double returnValue = Double.NEGATIVE_INFINITY;
				int count = 0;
				boolean hasValue = false;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.max( returnValue, decode( values.nextValue() ) );
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalDouble.of( returnValue );
				}
				break;
			}
			case MEDIAN: {
				List<Double> all = list( values, scores, docItr, startDoc, endDoc, maxChildren );

				if ( all.isEmpty() ) {
					break;
				}

				int count = all.size();

				if ( count > 0 ) {
					Collections.sort( all );
					double returnValue;
					if ( count % 2 == 0 ) {
						int pos = count / 2;
						returnValue = (all.get( pos - 1 ) + all.get( pos )) / 2;
					}
					else {
						int pos = count / 2;
						returnValue = all.get( pos );
					}
					result = OptionalDouble.of( returnValue );
				}

				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		if ( !result.isPresent() && scores.advanceExact( endDoc ) ) {
			result = OptionalDouble.of( scores.doubleValue() );
		}

		return result;
	}

	protected List<Double> list(SortedNumericDocValues values, DoubleValues scores, DocIdSetIterator docItr, int startDoc, int endDoc,
		int maxChildren) throws IOException {

		List<Double> result = new ArrayList<>();
		int count = 0;
		for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
			if ( values.advanceExact( doc ) ) {
				if ( ++count > maxChildren ) {
					break;
				}
				result.add( decode( values.nextValue() ) );
			}
		}

		Collections.sort( result );

		if ( !result.isEmpty() && scores.advanceExact( endDoc ) ) {
			result = Collections.singletonList( scores.doubleValue() );
		}

		return result;
	}

	protected DoubleMultiValues replaceScores(DoubleValues values, DoubleValues scores) {
		return new DoubleMultiValues() {

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
			public double doubleValue() throws IOException {
				return value;
			}
		};
	}

	private static class MultiFieldValuesSource extends DoubleMultiValuesSource {

		private MultiFieldValuesSource(String field, MultiValueMode mode,
			LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
			super( field, mode, decoder, encoder );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MultiFieldValuesSource that = (MultiFieldValuesSource) o;
			return Objects.equals( field, that.field )
				&& Objects.equals( decoder, that.decoder );
		}

		@Override
		public String toString() {
			return "double(" + field + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash( field, decoder );
		}

		@Override
		public DoubleMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
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

	}

	private static class NestedMultiFieldValuesSource extends DoubleMultiValuesSource {

		private final NestedDocsProvider nested;

		private NestedMultiFieldValuesSource(String field, MultiValueMode mode, NestedDocsProvider nested,
			LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
			super( field, mode, decoder, encoder );
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
			MultiFieldValuesSource that = (MultiFieldValuesSource) o;
			return Objects.equals( field, that.field )
				&& Objects.equals( decoder, that.decoder );
		}

		@Override
		public String toString() {
			return "double(" + field + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash( field, decoder );
		}

		@Override
		public DoubleMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
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

	private static class LongDoubleValuesSource extends LongMultiValuesSource {

		private final DoubleMultiValuesSource inner;
		private final DoubleToLongFunction encoder;

		public LongDoubleValuesSource(DoubleMultiValuesSource inner, String field, MultiValueMode mode, DoubleToLongFunction encoder) {
			super( field, mode );
			this.inner = inner;
			this.encoder = encoder;
		}

		@Override
		public LongMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			DoubleMultiValues in = inner.getValues( ctx, scores );
			return new LongMultiValues() {
				@Override
				public long longValue() throws IOException {
					return encoder.applyAsLong( in.doubleValue() );
				}

				@Override
				public long nextValue() throws IOException {
					return encoder.applyAsLong( in.nextValue() );
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return in.advanceExact( doc );
				}

				@Override
				public int docValueCount() {
					return in.docValueCount();
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
