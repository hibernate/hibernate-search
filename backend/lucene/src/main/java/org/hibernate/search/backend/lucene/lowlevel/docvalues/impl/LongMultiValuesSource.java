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
import java.util.OptionalLong;
import java.util.function.DoubleToLongFunction;
import java.util.function.LongToDoubleFunction;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValues;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.util.BitSet;
import org.hibernate.search.backend.lucene.lowlevel.join.impl.NestedDocsProvider;

public abstract class LongMultiValuesSource extends LongValuesSource {

	/**
	 * Returns a {@link LongValues} instance for the passed-in LeafReaderContext and scores
	 *
	 * If scores are not needed to calculate the values (ie {@link #needsScores() returns false}, callers
	 * may safely pass {@code null} for the {@code scores} parameter.
	 *
	 * @param ctx the context
	 * @param scores the scores
	 * @return the long multi value
	 * @throws java.io.IOException
	 */
	@Override
	public abstract LongMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException;

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
	 * Creates a LongMultiValuesSource that wraps a long-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @return DoubleMultiValuesSource
	 */
	public static LongMultiValuesSource fromLongField(String field, MultiValueMode mode) {
		return fromLongField( field, mode, null );
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
	 * Creates a LongMultiValuesSource that wraps an int-valued field
	 *
	 * @param field the field
	 * @param mode the mode
	 * @return DoubleMultiValuesSource
	 */
	public static LongMultiValuesSource fromIntField(String field, MultiValueMode mode) {
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
		return new RawNumericDocValues( getValues( ctx, scores ) );
	}

	protected LongMultiValues select(final SortedNumericDocValues values, final DoubleValues scores) {
		final NumericDocValues singleton = DocValues.unwrapSingleton( values );
		if ( singleton != null ) {
			return replaceScores( new LongMultiValues() {

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
		else if ( mode == null || mode == MultiValueMode.NONE ) {
			return replaceScores( new LongMultiValues() {

				@Override
				public long longValue() throws IOException {
					return values.nextValue();
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
			return new LongMultiValues() {
				private OptionalLong value = OptionalLong.empty();

				@Override
				public long longValue() throws IOException {
					return value.getAsLong();
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					value = OptionalLong.empty();
					if ( values.advanceExact( doc ) ) {
						value = pick( values, scores, doc );
						return true;
					}
					else if ( scores != null && scores.advanceExact( doc ) ) {
						value = OptionalLong.of( (long) scores.doubleValue() );
					}
					return false;
				}
			};
		}
	}

	protected LongMultiValues select(final SortedNumericDocValues values, final DoubleValues scores, final BitSet parentDocs,
		final DocIdSetIterator childDocs, int maxDoc, int maxChildren) throws IOException {
		if ( parentDocs == null || childDocs == null ) {
			return replaceScores( DoubleValues.EMPTY, scores );
		}

		return new LongMultiValues() {
			int lastSeenParentDoc = -1;
			long lastEmittedValue = -1;
			private Iterator<Long> value;
			private List<Long> all;

			@Override
			public long longValue() throws IOException {
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
						all = Collections.singletonList( (long) scores.doubleValue() );
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
					OptionalLong pick = pick( values, scores, childDocs, firstChildDoc, parentDoc, maxChildren );
					if ( pick.isPresent() ) {
						all = Collections.singletonList( pick.getAsLong() );
					}
					else if ( scores != null && scores.advanceExact( parentDoc ) ) {
						all = Collections.singletonList( (long) scores.doubleValue() );
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

	protected OptionalLong pick(SortedNumericDocValues values, DoubleValues scores, int doc) throws IOException {
		final int count = values.docValueCount();
		OptionalLong result = OptionalLong.empty();

		switch ( mode ) {
			case SUM: {
				boolean hasValue = false;
				long returnValue = 0;
				for ( int index = 0; index < count; ++index ) {
					returnValue += values.nextValue();
					hasValue = true;
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case AVG: {
				boolean hasValue = false;
				long returnValue = 0;
				for ( int index = 0; index < count; ++index ) {
					returnValue += values.nextValue();
					hasValue = true;
				}
				returnValue = returnValue / count;

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case MIN: {
				boolean hasValue = false;
				long returnValue = Long.MAX_VALUE;
				for ( int index = 0; index < count; ++index ) {
					returnValue = Math.min( returnValue, values.nextValue() );
					hasValue = true;
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case MAX: {
				boolean hasValue = false;
				long returnValue = Long.MIN_VALUE;
				for ( int index = 0; index < count; ++index ) {
					returnValue = Math.max( returnValue, values.nextValue() );
					hasValue = true;
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case MEDIAN: {
				boolean hasValue = false;
				long returnValue = 0;

				if ( count > 0 ) {
					for ( int i = 0; i < (count - 1) / 2; ++i ) {
						values.nextValue();
					}
					if ( count % 2 == 0 ) {
						returnValue = (values.nextValue() + values.nextValue()) / 2;
						hasValue = true;
					}
					else {
						returnValue = values.nextValue();
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		if ( !result.isPresent() && scores.advanceExact( doc ) ) {
			result = OptionalLong.of( (long) scores.doubleValue() );
		}

		return result;
	}

	protected OptionalLong pick(SortedNumericDocValues values, DoubleValues scores, DocIdSetIterator docItr, int startDoc, int endDoc,
		int maxChildren) throws IOException {

		OptionalLong result = OptionalLong.empty();

		switch ( mode ) {
			case SUM: {
				int count = 0;
				long returnValue = 0;
				boolean hasValue = false;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						final int docCount = values.docValueCount();
						for ( int index = 0; index < docCount; ++index ) {
							returnValue += values.nextValue();
						}
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case AVG: {
				int count = 0;
				int totalCount = 0;
				long returnValue = 0;
				boolean hasValue = false;
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

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case MIN: {
				long returnValue = Long.MAX_VALUE;
				int count = 0;
				boolean hasValue = false;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.min( returnValue, values.nextValue() );
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case MAX: {
				long returnValue = Long.MIN_VALUE;
				int count = 0;
				boolean hasValue = false;
				for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
					if ( values.advanceExact( doc ) ) {
						if ( ++count > maxChildren ) {
							break;
						}
						returnValue = Math.max( returnValue, values.nextValue() );
						hasValue = true;
					}
				}

				if ( hasValue ) {
					result = OptionalLong.of( returnValue );
				}
				break;
			}
			case MEDIAN: {
				int count = 0;
				List<Long> all = list( values, scores, docItr, startDoc, endDoc, maxChildren );
				if ( all.isEmpty() ) {
					break;
				}

				count = all.size();

				if ( count > 0 ) {
					long returnValue;
					if ( count % 2 == 0 ) {
						int pos = count / 2;
						returnValue = (all.get( pos - 1 ) + all.get( pos )) / 2;
					}
					else {
						int pos = count / 2;
						returnValue = all.get( pos );
					}
					result = OptionalLong.of( returnValue );
				}

				break;
			}
			default:
				throw new IllegalArgumentException( "Unsupported sort mode: " + mode );
		}

		if ( !result.isPresent() && scores.advanceExact( endDoc ) ) {
			result = OptionalLong.of( (long) scores.doubleValue() );
		}

		return result;
	}

	protected List<Long> list(SortedNumericDocValues values, DoubleValues scores, DocIdSetIterator docItr, int startDoc, int endDoc,
		int maxChildren) throws IOException {

		List<Long> result = new ArrayList<>();
		int count = 0;
		for ( int doc = startDoc; doc < endDoc; doc = docItr.nextDoc() ) {
			if ( values.advanceExact( doc ) ) {
				if ( ++count > maxChildren ) {
					break;
				}
				result.add( values.nextValue() );
			}
		}

		Collections.sort( result );

		if ( !result.isEmpty() && scores.advanceExact( endDoc ) ) {
			result = Collections.singletonList( (long) scores.doubleValue() );
		}

		return result;
	}

	protected LongMultiValues replaceScores(LongMultiValues values, DoubleValues scores) {
		return new LongMultiValues() {

			private Long value;
			private boolean singleton = false;

			@Override
			public boolean advanceExact(int target) throws IOException {
				singleton = false;

				if ( values.advanceExact( target ) ) {
					value = values.longValue();
				}
				else if ( scores != null && scores.advanceExact( target ) ) {
					value = (long) scores.doubleValue();
					singleton = true;
				}
				return false;
			}

			@Override
			public long longValue() throws IOException {
				return nextValue();
			}

			@Override
			public long nextValue() throws IOException {
				if ( !singleton ) {
					return values.nextValue();
				}
				return value;
			}

			@Override
			public int docValueCount() {
				return 1;
			}

		};
	}

	protected LongMultiValues replaceScores(DoubleValues values, DoubleValues scores) {
		return new LongMultiValues() {

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

	@Override
	public DoubleMultiValuesSource toDoubleValuesSource() {
		return new DoubleMultiLongValuesSource( this, field, mode, Double::longBitsToDouble, Double::doubleToRawLongBits );
	}

	public DoubleMultiValuesSource toFloatValuesSource() {
		return new DoubleMultiLongValuesSource( this, field, mode, (v) -> (double) Float.intBitsToFloat( (int) v ), (v) -> (long) Float.floatToRawIntBits( (float) v ) );
	}

	private static class DoubleMultiLongValuesSource extends DoubleMultiValuesSource {

		private final LongMultiValuesSource inner;

		private DoubleMultiLongValuesSource(LongMultiValuesSource inner, String field, MultiValueMode mode, LongToDoubleFunction decoder, DoubleToLongFunction encoder) {
			super( field, mode, decoder, encoder );
			this.inner = inner;
		}

		@Override
		public DoubleMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
			LongMultiValues v = inner.getValues( ctx, scores );
			return new DoubleMultiValues() {
				@Override
				public double doubleValue() throws IOException {
					return decode( v.longValue() );
				}

				@Override
				public boolean advanceExact(int doc) throws IOException {
					return v.advanceExact( doc );
				}

				@Override
				public double nextValue() throws IOException {
					return decode( v.nextValue() );
				}

				@Override
				public int docValueCount() {
					return v.docValueCount();
				}
			};
		}

		@Override
		public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
			return inner.rewrite( searcher ).toDoubleValuesSource();
		}

		@Override
		public boolean isCacheable(LeafReaderContext ctx) {
			return inner.isCacheable( ctx );
		}

		@Override
		public String toString() {
			return "double(" + inner.toString() + ")";
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
			DoubleMultiLongValuesSource that = (DoubleMultiLongValuesSource) o;
			return Objects.equals( inner, that.inner );
		}

		@Override
		public int hashCode() {
			return Objects.hash( inner );
		}
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
		public LongMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
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
		public LongMultiValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
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
